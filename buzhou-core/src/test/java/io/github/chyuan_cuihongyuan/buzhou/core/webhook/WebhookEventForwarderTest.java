package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 事件外发 webhook 持久化 outbox 测试（spec 20 / T89 回归 + spec 24 / T103 / impl-78）：
 * JDK HttpServer 收件断言 payload JSON / HMAC 签名 / 幂等键；持久化矩阵——重启恢复 /
 * 退避落 store / 死信隔离 / 4xx 即死 / 容量拒入 / 成功即删。
 */
class WebhookEventForwarderTest {

    HttpServer server;
    WebhookEventForwarder forwarder;

    @AfterEach
    void tearDown() {
        if (forwarder != null) {
            forwarder.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    record Received(String body, String signature, String eventId, int status) {
    }

    private static final class Collector {
        final ConcurrentLinkedQueue<Received> received = new ConcurrentLinkedQueue<>();
        volatile int status = 200;
        volatile int failFirstN = 0;
        volatile int hits = 0;
    }

    /** 基本投递（spec 20 回归）：payload JSON 结构 + 签名头 + 幂等键头；成功即从 outbox 删除。 */
    @Test
    void deliversSignedJsonEnvelope() throws Exception {
        Collector collector = new Collector();
        startServer(collector);
        SessionStateStore store = new InMemorySessionStateStore();
        forwarder = newForwarder(store, "topsecret", 2, 100);

        forwarder.onEvent(SessionEvent.of("user.turn.completed", Map.of("sessionId", "s1")));

        await(() -> !collector.received.isEmpty());
        Received first = collector.received.peek();
        assertThat(first.body()).contains("\"type\":\"user.turn.completed\"")
                .contains("\"sessionId\":\"s1\"").contains("\"eventId\":\"" + first.eventId() + "\"");
        assertThat(first.eventId()).isNotBlank();
        assertThat(first.signature())
                .isEqualTo(WebhookEventForwarder.hmacSha256("topsecret", first.body()));
        assertThat(forwarder.delivered()).isEqualTo(1);
        await(() -> forwarder.pendingCount() == 0);
        assertThat(store.getAll(WebhookOutbox.SESSION_ID).keySet())
                .noneMatch(k -> k.startsWith(WebhookOutbox.OUTBOX_PREFIX));
    }

    /** 5xx 先拒后收（spec 20 回归）：退避重试内送达（attempts 状态落 store）。 */
    @Test
    void retriesOn5xxThenSucceeds() throws Exception {
        Collector collector = new Collector();
        collector.failFirstN = 1; // 首次 500，之后 200
        startServer(collector);
        forwarder = newForwarder(new InMemorySessionStateStore(), null, 3, 100);

        forwarder.onEvent(SessionEvent.of("retry.me", Map.of()));

        await(() -> forwarder.delivered() == 1); // 服务端先收到、端上后计数——以端上为准
        assertThat(collector.hits).isEqualTo(2); // 1 次失败 + 1 次成功
        assertThat(forwarder.failed()).isZero();
        assertThat(forwarder.deadLetters()).isEmpty();
    }

    /** 4xx 即死（spec 24）：单次命中进死信 + failures/dead-letter 计数，不再重试。 */
    @Test
    void deadLetters4xxImmediately() throws Exception {
        Collector collector = new Collector();
        collector.status = 404;
        startServer(collector);
        forwarder = newForwarder(new InMemorySessionStateStore(), null, 3, 100);

        forwarder.onEvent(SessionEvent.of("nope", Map.of()));

        await(() -> forwarder.deadLettered() == 1);
        Thread.sleep(300); // 若误重试会有更多命中
        assertThat(collector.hits).isEqualTo(1);
        assertThat(forwarder.delivered()).isZero();
        assertThat(forwarder.failed()).isEqualTo(1);
        List<WebhookDeadLetter> dead = forwarder.deadLetters();
        assertThat(dead).hasSize(1);
        assertThat(dead.get(0).type()).isEqualTo("nope");
        assertThat(dead.get(0).attempts()).isEqualTo(1);
    }

    /** 重试耗尽进死信（spec 24）：attempts 达上限隔离，不再重试。 */
    @Test
    void deadLettersAfterMaxAttempts() throws Exception {
        Collector collector = new Collector();
        collector.status = 500; // 恒 5xx
        startServer(collector);
        forwarder = newForwarder(new InMemorySessionStateStore(), null, 2, 100);

        forwarder.onEvent(SessionEvent.of("always.fails", Map.of()));

        await(() -> forwarder.deadLettered() == 1);
        assertThat(collector.hits).isEqualTo(2); // 2 次尝试后隔离
        List<WebhookDeadLetter> dead = forwarder.deadLetters();
        assertThat(dead).hasSize(1);
        assertThat(dead.get(0).attempts()).isEqualTo(2);
        assertThat(forwarder.pendingCount()).isZero();
    }

    /** 跨重启恢复（spec 24 核心）：forwarder 实例重建（模拟进程重启），共享 store 的
     *  未决事件由新实例补投递——退避状态持久化在记录里，重启不丢。 */
    @Test
    void recoversPendingRecordsAcrossRestart() throws Exception {
        Collector collector = new Collector();
        collector.status = 500; // 第一代：恒失败，事件留在 outbox
        startServer(collector);
        SessionStateStore store = new InMemorySessionStateStore();
        forwarder = newForwarder(store, null, 8, 100);

        forwarder.onEvent(SessionEvent.of("survive.restart", Map.of("sessionId", "s9")));
        await(() -> collector.hits >= 1); // 首试失败
        forwarder.close(); // 未到期退避记录留存 store

        collector.status = 200; // 第二代：下游恢复
        forwarder = newForwarder(store, null, 8, 100);

        await(() -> forwarder.delivered() == 1);
        await(() -> forwarder.pendingCount() == 0);
        Received body = collector.received.peek();
        assertThat(body.body()).contains("\"type\":\"survive.restart\"");
    }

    /** 容量上限（spec 24）：未决满则拒入 + dropped 计数（不阻塞主链）。 */
    @Test
    void rejectsWhenOutboxFull() throws Exception {
        Collector collector = new Collector();
        collector.status = 500; // 恒失败让记录滞留未决
        startServer(collector);
        forwarder = newForwarder(new InMemorySessionStateStore(), null, 2, 1);

        forwarder.onEvent(SessionEvent.of("occupies.slot", Map.of()));
        forwarder.onEvent(SessionEvent.of("overflow", Map.of()));

        assertThat(forwarder.dropped()).isEqualTo(1);
        assertThat(forwarder.pendingCount()).isEqualTo(1);
        await(() -> forwarder.deadLettered() == 1); // 槽内记录走完生命周期
        assertThat(forwarder.deadLetters()).hasSize(1);
    }

    /** 死信重放（spec 37 §B / T133）：耗尽死信 → 端点恢复 → replayDeadLetters 一键补投。 */
    @Test
    void replaysDeadLettersAfterEndpointRecovers() throws Exception {
        Collector collector = new Collector();
        collector.status = 500;
        startServer(collector);
        forwarder = newForwarder(new InMemorySessionStateStore(), null, 2, 100);

        forwarder.onEvent(SessionEvent.of("will.die", Map.of()));
        await(() -> forwarder.deadLettered() == 1);
        assertThat(forwarder.deadLetters()).hasSize(1);
        int deadAtFirst = forwarder.deadLetters().size();

        collector.status = 200; // 端点恢复
        assertThat(forwarder.replayDeadLetters()).isEqualTo(1);
        await(() -> forwarder.delivered() == 1);
        await(() -> forwarder.pendingCount() == 0);
        assertThat(forwarder.deadLetters()).isEmpty(); // 死信区清空
        assertThat(collector.received.peek().body()).contains("will.die"); // 消费端终见事件
        assertThat(deadAtFirst).isEqualTo(1);
    }

    // ---- helpers ----

    private WebhookEventForwarder newForwarder(SessionStateStore store, String secret,
            int maxAttempts, int outboxCapacity) {
        return new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                secret, Duration.ofMillis(2000), maxAttempts, outboxCapacity, null), store);
    }

    private void startServer(Collector collector) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            collector.hits++;
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            List<String> sig = exchange.getRequestHeaders().get("X-Buzhou-Signature");
            List<String> id = exchange.getRequestHeaders().get("X-Buzhou-Event-Id");
            int status = collector.hits <= collector.failFirstN ? 500 : collector.status;
            if (status == 200) {
                collector.received.add(new Received(body,
                        sig == null ? null : sig.get(0), id == null ? null : id.get(0), status));
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
    }

    private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("等待超时");
            }
            Thread.sleep(20);
        }
    }
}
