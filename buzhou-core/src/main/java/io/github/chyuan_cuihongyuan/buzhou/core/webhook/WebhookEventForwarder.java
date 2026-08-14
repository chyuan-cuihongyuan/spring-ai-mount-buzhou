package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件外发 webhook（spec 20 / T89 / impl-64）：SessionEvent 旁路转发到外部 HTTP 端点。
 *
 * <p><b>投递语义</b>：at-least-once（幂等键 {@code eventId}=UUID，每请求带
 * {@code X-Buzhou-Event-Id}，消费方按需去重；exactly-once 不承诺）。单虚拟线程分发器 +
 * 有界队列（满则<b>丢弃 + 计数</b>，绝不阻塞会话事件主链）；close 限时排空。
 *
 * <p><b>签名</b>：配置 secret 时每请求带 {@code X-Buzhou-Signature:
 * hex(HMAC-SHA256(secret, body))}。<b>重试</b>：IOException / 5xx 退避 1s×2^n（上限
 * max-attempts）；4xx 不重试（配置/消费端错误）。全败丢弃 + 计数。
 *
 * <p>挂点：core auto-config 收集 {@link SessionEventListener} bean 经
 * {@code DefaultAgentRuntime.addGlobalEventListener} 挂到全部会话（spec 20）。
 */
public final class WebhookEventForwarder implements SessionEventListener, AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(WebhookEventForwarder.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BuzhouWebhookProperties props;
    private final HttpClient http;
    private final BlockingQueue<String> queue;
    private final Thread dispatcher;
    private final CountDownLatch stopped = new CountDownLatch(1);
    private final AtomicLong delivered = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    volatile boolean closing;

    public WebhookEventForwarder(BuzhouWebhookProperties props) {
        this.props = props;
        this.http = HttpClient.newBuilder().connectTimeout(props.timeout()).build();
        this.queue = new ArrayBlockingQueue<>(props.queueCapacity());
        this.dispatcher = Thread.ofVirtual().name("buzhou-webhook-dispatcher").unstarted(this::dispatchLoop);
        this.dispatcher.start();
    }

    @Override
    public void onEvent(SessionEvent event) {
        if (closing) {
            return;
        }
        String body;
        try {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("eventId", UUID.randomUUID().toString());
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
        if (!queue.offer(body)) {
            dropped.incrementAndGet();
            BuzhouMetricsHolder.metrics().counter("buzhou.webhook.dropped");
        }
    }

    private void dispatchLoop() {
        try {
            while (!closing || !queue.isEmpty()) {
                String body = queue.poll(200, TimeUnit.MILLISECONDS);
                if (body == null) {
                    continue;
                }
                deliverWithRetry(body);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            stopped.countDown();
        }
    }

    private void deliverWithRetry(String body) {
        long backoffMillis = 1000;
        for (int attempt = 1; attempt <= props.maxAttempts(); attempt++) {
            try {
                HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(props.url()))
                        .timeout(props.timeout())
                        .header("Content-Type", "application/json")
                        .header("X-Buzhou-Event-Id", eventIdOf(body))
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                if (props.secret() != null && !props.secret().isBlank()) {
                    request.header("X-Buzhou-Signature", hmacSha256(props.secret(), body));
                }
                HttpResponse<Void> response = http.send(request.build(), HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    delivered.incrementAndGet();
                    BuzhouMetricsHolder.metrics().counter("buzhou.webhook.delivered");
                    return;
                }
                if (response.statusCode() >= 400 && response.statusCode() < 500) {
                    break; // 4xx：配置/消费端错误，重试无意义
                }
                // 5xx：可重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception retryable) {
                // IOException / 连接失败：可重试
            }
            if (attempt < props.maxAttempts()) {
                try {
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                backoffMillis *= 2;
            }
        }
        failed.incrementAndGet();
        BuzhouMetricsHolder.metrics().counter("buzhou.webhook.failures");
        LOGGER.log(System.Logger.Level.ERROR, "webhook 投递失败（已重试 " + props.maxAttempts()
                + " 次），事件丢弃：url=" + props.url());
    }

    private static String eventIdOf(String body) {
        try {
            return MAPPER.readTree(body).path("eventId").asText("");
        } catch (Exception e) {
            return "";
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

    /** 优雅关闭：停分发循环前排空剩余队列（限时 5s）。 */
    @Override
    public void close() {
        closing = true;
        try {
            stopped.await(5, TimeUnit.SECONDS);
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

    public Duration timeout() {
        return props.timeout();
    }
}
