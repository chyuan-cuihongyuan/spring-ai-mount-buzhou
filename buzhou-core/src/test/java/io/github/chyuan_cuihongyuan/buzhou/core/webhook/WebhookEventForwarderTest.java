package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
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
 * 事件外发 webhook 测试（spec 20 / T89 / impl-64）：JDK HttpServer 收件断言
 * payload JSON / HMAC 签名 / 幂等键；5xx 重试后成功；4xx 不重试即弃。
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

    /** 基本投递：payload JSON 结构 + 签名头 + 幂等键头；at-least-once 送达。 */
    @Test
    void deliversSignedJsonEnvelope() throws Exception {
        Collector collector = new Collector();
        startServer(collector);
        forwarder = new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                "topsecret", Duration.ofMillis(2000), 2, 16));

        String body = pushEvent(forwarder, "user.turn.completed", Map.of("sessionId", "s1"));

        await(() -> !collector.received.isEmpty());
        Received first = collector.received.peek();
        assertThat(first.body()).contains("\"type\":\"user.turn.completed\"")
                .contains("\"sessionId\":\"s1\"").contains("\"eventId\":\"" + first.eventId() + "\"");
        assertThat(first.eventId()).isNotBlank();
        assertThat(first.signature())
                .isEqualTo(WebhookEventForwarder.hmacSha256("topsecret", first.body()));
        assertThat(forwarder.delivered()).isEqualTo(1);
    }

    /** 5xx 先拒后收：退避重试内送达（maxAttempts=3）。 */
    @Test
    void retriesOn5xxThenSucceeds() throws Exception {
        Collector collector = new Collector();
        collector.failFirstN = 1; // 首次 500，之后 200
        startServer(collector);
        forwarder = new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                null, Duration.ofMillis(1000), 3, 16));

        pushEvent(forwarder, "retry.me", Map.of());

        await(() -> !collector.received.isEmpty());
        assertThat(collector.hits).isEqualTo(2); // 1 次失败 + 1 次成功
        assertThat(forwarder.delivered()).isEqualTo(1);
        assertThat(forwarder.failed()).isZero();
    }

    /** 4xx 不重试：单次命中即弃 + failures 计数（等 2 个退避周期无新命中）。 */
    @Test
    void doesNotRetryOn4xx() throws Exception {
        Collector collector = new Collector();
        collector.status = 404;
        startServer(collector);
        forwarder = new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                null, Duration.ofMillis(1000), 3, 16));

        pushEvent(forwarder, "nope", Map.of());

        await(() -> forwarder.failed() == 1);
        Thread.sleep(300); // 若误重试会有更多命中
        assertThat(collector.hits).isEqualTo(1);
        assertThat(forwarder.delivered()).isZero();
    }

    // ---- helpers ----

    private static String pushEvent(WebhookEventForwarder forwarder, String type,
            Map<String, Object> payload) {
        // 直接复用 onEvent 的序列化路径：等 delivered/failed 前先入队
        forwarder.onEvent(SessionEvent.of(type, payload));
        return "";
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
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("等待超时");
            }
            Thread.sleep(20);
        }
    }
}
