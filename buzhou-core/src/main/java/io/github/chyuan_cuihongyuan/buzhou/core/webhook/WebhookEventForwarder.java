package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件外发 webhook（spec 20 / T89；持久化 outbox 升级 spec 24 / T103 / impl-78）：
 * SessionEvent 旁路转发到外部 HTTP 端点，跨重启不丢。
 *
 * <p><b>投递语义</b>：at-least-once（幂等键 {@code eventId}=UUID，每请求带
 * {@code X-Buzhou-Event-Id}，消费方按需去重；exactly-once 不承诺）。事件 emit 即**同步**
 * 落 {@link WebhookOutbox}（{@link SessionStateStore} 合成会话，JDBC/Redis store 持久化、
 * 重启自动恢复重放）；单虚拟线程分发器轮询「到期」记录，每轮单试——退避状态
 * （attempts/nextAttemptAt）持久化在记录里，由后续轮次自然拾起，不在循环内阻塞睡眠。
 *
 * <p><b>重试与死信</b>：IOException/5xx 退避 {@code min(1s×2^attempts, 60s)}；4xx 即死
 * （配置/消费端错误）；单条尝试达 {@code max-attempts}（默认 8）进死信
 * （{@link #deadLetters()} 可查），指标 {@code buzhou.webhook.dead-letter} +
 * {@code buzhou.webhook.failures}；成功即删。
 *
 * <p><b>容量</b>：未决记录达 {@code outbox-capacity}（默认 10_000）→ 新事件拒入 +
 * {@code buzhou.webhook.dropped} 计数（不阻塞会话主链）。多实例共享 store 可能双投递——
 * 消费端幂等键去重是契约责任（runbook §6）。
 *
 * <p><b>签名</b>：配置 secret 时每请求带 {@code X-Buzhou-Signature:
 * hex(HMAC-SHA256(secret, body))}。HTTP：JDK HttpClient（零新依赖）。
 *
 * <p>挂点：core auto-config 收集 {@link SessionEventListener} bean 经
 * {@code DefaultAgentRuntime.addGlobalEventListener} 挂到全部会话（spec 20）。
 */
public final class WebhookEventForwarder implements SessionEventListener, AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(WebhookEventForwarder.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int BATCH = 32;
    private static final long BACKOFF_BASE_MILLIS = 1000;
    private static final long BACKOFF_CAP_MILLIS = 60_000;
    private static final int DEAD_LETTER_QUERY_LIMIT = 100;

    /**
     * spec 50 §B / T179 / impl-148：退避抖动随机源（±25%，防多实例同相位雷鸣羊群）。
     * 生产默认 ThreadLocalRandom；确定性测试直接调 {@link #jitteredBackoffMillis}。
     */
    private final java.util.function.DoubleSupplier jitterRandom =
            java.util.concurrent.ThreadLocalRandom.current()::nextDouble;

    /** spec 50 §B：带 ±25% 抖动的退避（base=1s×2^attempts 封顶 60s；抖动后 ∈ [0.75, 1.25]×base）。 */
    static long jitteredBackoffMillis(int attempts, java.util.function.DoubleSupplier random) {
        long base = Math.min(BACKOFF_BASE_MILLIS << attempts, BACKOFF_CAP_MILLIS);
        double factor = 0.75 + 0.5 * random.getAsDouble();
        return Math.round(base * factor);
    }

    private final BuzhouWebhookProperties props;
    private final HttpClient http;
    private final WebhookOutbox outbox;
    private final Semaphore nudge = new Semaphore(0);
    private final Thread dispatcher;
    private final CountDownLatch stopped = new CountDownLatch(1);
    private final AtomicLong delivered = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong deadLettered = new AtomicLong();
    volatile boolean closing;

    public WebhookEventForwarder(BuzhouWebhookProperties props, SessionStateStore stateStore) {
        this.props = props;
        this.http = HttpClient.newBuilder().connectTimeout(props.timeout()).build();
        this.outbox = new WebhookOutbox(stateStore, props.outboxCapacity());
        this.dispatcher = Thread.ofVirtual().name("buzhou-webhook-dispatcher").unstarted(this::dispatchLoop);
        this.dispatcher.start();
    }

    @Override
    public void onEvent(SessionEvent event) {
        if (closing) {
            return;
        }
        String eventId = UUID.randomUUID().toString();
        String body;
        try {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("eventId", eventId);
            envelope.put("type", event.type());
            envelope.put("payload", event.payload());
            envelope.put("occurredAt", event.occurredAt().toString());
            Object sessionId = event.payload().get("sessionId");
            if (sessionId != null) {
                envelope.put("sessionId", sessionId);
            }
            body = MAPPER.writeValueAsString(envelope);
        } catch (Exception e) {
            return; // 序列化失败：单事件丢弃（不可投递），不计 webhook 故障
        }
        if (!outbox.append(eventId, event.type(), body)) {
            dropped.incrementAndGet();
            BuzhouMetricsHolder.metrics().counter("buzhou.webhook.dropped");
            LOGGER.log(System.Logger.Level.WARNING,
                    "webhook outbox 满（capacity=" + props.outboxCapacity() + "），事件拒入队");
            return;
        }
        nudge.release();
    }

    private void dispatchLoop() {
        try {
            while (!closing) {
                if (!processDueBatch()) {
                    nudge.tryAcquire(200, TimeUnit.MILLISECONDS);
                }
            }
            drainDueBeforeClose();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            stopped.countDown();
        }
    }

    /** 优雅关闭：限时（可配 close-drain-timeout，默认 5s）排空「已到期」记录；未到期退避记录留存 store，由下次启动恢复。 */
    private void drainDueBeforeClose() {
        long deadline = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(props.effectiveCloseDrainTimeout().toMillis());
        while (System.nanoTime() < deadline && !outbox.due(Instant.now(), 1).isEmpty()) {
            processDueBatch();
        }
    }

    private boolean processDueBatch() {
        List<WebhookOutbox.OutboxRecord> due = outbox.due(Instant.now(), BATCH);
        for (WebhookOutbox.OutboxRecord record : due) {
            Outcome outcome = attemptOnce(record);
            switch (outcome) {
                case DELIVERED -> {
                    outbox.delete(record.eventId());
                    delivered.incrementAndGet();
                    BuzhouMetricsHolder.metrics().counter("buzhou.webhook.delivered");
                }
                case FATAL -> markDead(record, "4xx");
                case RETRYABLE -> scheduleRetryOrDead(record);
                default -> throw new IllegalStateException("unreachable: " + outcome);
            }
        }
        return !due.isEmpty();
    }

    private void scheduleRetryOrDead(WebhookOutbox.OutboxRecord record) {
        int attempts = record.attempts() + 1;
        if (attempts >= props.maxAttempts()) {
            markDead(record, "重试耗尽");
            return;
        }
        long backoff = jitteredBackoffMillis(attempts, jitterRandom);
        outbox.update(new WebhookOutbox.OutboxRecord(record.eventId(), record.type(), record.body(),
                record.seq(), attempts, System.currentTimeMillis() + backoff, record.createdAtEpochMs()));
    }

    private void markDead(WebhookOutbox.OutboxRecord record, String reason) {
        int totalAttempts = record.attempts() + 1;
        outbox.markDead(new WebhookOutbox.OutboxRecord(record.eventId(), record.type(), record.body(),
                record.seq(), totalAttempts, record.nextAttemptAtEpochMs(), record.createdAtEpochMs()));
        failed.incrementAndGet();
        deadLettered.incrementAndGet();
        BuzhouMetricsHolder.metrics().counter("buzhou.webhook.failures");
        BuzhouMetricsHolder.metrics().counter("buzhou.webhook.dead-letter");
        LOGGER.log(System.Logger.Level.ERROR, "webhook 事件进死信（" + reason + "，attempts="
                + totalAttempts + "）：eventId=" + record.eventId() + " url=" + props.url());
    }

    private enum Outcome {DELIVERED, FATAL, RETRYABLE}

    /** 单次尝试：2xx 成功；4xx 致命；IOException/5xx 可重试。 */
    private Outcome attemptOnce(WebhookOutbox.OutboxRecord record) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(props.url()))
                    .timeout(props.timeout())
                    .header("Content-Type", "application/json")
                    .header("X-Buzhou-Event-Id", record.eventId())
                    .POST(HttpRequest.BodyPublishers.ofString(record.body(), StandardCharsets.UTF_8));
            if (props.secret() != null && !props.secret().isBlank()) {
                request.header("X-Buzhou-Signature", hmacSha256(props.secret(), record.body()));
            }
            HttpResponse<Void> response = http.send(request.build(), HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Outcome.DELIVERED;
            }
            if (response.statusCode() >= 400 && response.statusCode() < 500) {
                return Outcome.FATAL;
            }
            return Outcome.RETRYABLE; // 5xx
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Outcome.FATAL; // 停机中断：不再重试本轮
        } catch (Exception retryable) {
            return Outcome.RETRYABLE; // IOException / 连接失败
        }
    }

    static String hmacSha256(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 不可用", e);
        }
    }

    /** 优雅关闭：停止接新事件，限时 5s 排空已到期记录（退避中的留存 store 待重启恢复）。 */
    @Override
    public void close() {
        closing = true;
        nudge.release();
        try {
            stopped.await(props.effectiveCloseDrainTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ---- 观测（测试/运维） ----

    public long delivered() {
        return delivered.get();
    }

    public long dropped() {
        return dropped.get();
    }

    public long failed() {
        return failed.get();
    }

    public long deadLettered() {
        return deadLettered.get();
    }

    /** 死信查询（上限 100；重放由运维按需自建，spec 24 out-of-scope）。 */
    public List<WebhookDeadLetter> deadLetters() {
        return outbox.deadLetters(DEAD_LETTER_QUERY_LIMIT);
    }

    public int pendingCount() {
        return outbox.pendingCount();
    }

    /**
     * spec 37 §B / T133 / impl-106：一键重放全部死信（迁回 outbox、attempts 清零、
     * 立即触发投递）。投递语义回到常规（可能再死信——消费端幂等键去重契约内）。
     *
     * @return 本次重放的条数（容量满则部分重放）
     */
    public int replayDeadLetters() {
        int requeued = outbox.requeueDead(Integer.MAX_VALUE);
        if (requeued > 0) {
            nudge.release();
            LOGGER.log(System.Logger.Level.INFO,
                    "webhook 死信重放 " + requeued + " 条（attempts 清零，立即重投）");
        }
        return requeued;
    }

    public Duration timeout() {
        return props.timeout();
    }
}
