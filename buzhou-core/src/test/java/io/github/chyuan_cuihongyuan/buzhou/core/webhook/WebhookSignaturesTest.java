package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 428 §Testing / T747–T748：验签与防重放——常量时间 verify 正负样本、
 * 时间戳容差窗（新鲜过/窗外拒/非数字拒）、forwarder 加发 X-Buzhou-Timestamp、
 * 本地服务签名↔验签往返闭环。
 */
class WebhookSignaturesTest {

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    void shouldVerifyConstantTimeWithFailClosedSemantics() {
        String secret = "whsec";
        String body = "{\"a\":1}";
        String signature = WebhookSignatures.sign(secret, body);

        assertThat(WebhookSignatures.verify(secret, body, signature)).isTrue();
        // 错 secret / 篡改 body / 垃圾 hex / null——fail-closed 全 false
        assertThat(WebhookSignatures.verify("wrong", body, signature)).isFalse();
        assertThat(WebhookSignatures.verify(secret, "{\"a\":2}", signature)).isFalse();
        assertThat(WebhookSignatures.verify(secret, body, "not-hex")).isFalse();
        assertThat(WebhookSignatures.verify(null, body, signature)).isFalse();
        assertThat(WebhookSignatures.verify(secret, null, signature)).isFalse();
        assertThat(WebhookSignatures.verify(secret, body, null)).isFalse();
    }

    @Test
    void shouldBoundReplayWindowByTimestampTolerance() {
        String secret = "whsec";
        String body = "payload";
        String signature = WebhookSignatures.sign(secret, body);
        Instant now = Instant.now();
        Duration tolerance = Duration.ofMinutes(5);

        // 新鲜 + 窗内（4 分钟前）
        assertThat(WebhookSignatures.verify(secret, body, signature,
                String.valueOf(now.getEpochSecond()), tolerance, now)).isTrue();
        assertThat(WebhookSignatures.verify(secret, body, signature,
                String.valueOf(now.minus(Duration.ofMinutes(4)).getEpochSecond()), tolerance, now)).isTrue();
        // 窗外（6 分钟前）——重放拒
        assertThat(WebhookSignatures.verify(secret, body, signature,
                String.valueOf(now.minus(Duration.ofMinutes(6)).getEpochSecond()), tolerance, now)).isFalse();
        // 未来漂移同样有界
        assertThat(WebhookSignatures.verify(secret, body, signature,
                String.valueOf(now.plus(Duration.ofMinutes(6)).getEpochSecond()), tolerance, now)).isFalse();
        // 非数字 / 缺失时间戳——fail-closed
        assertThat(WebhookSignatures.verify(secret, body, signature, "abc", tolerance, now)).isFalse();
        assertThat(WebhookSignatures.verify(secret, body, signature, null, tolerance, now)).isFalse();
    }

    private static final class Received {
        final String body;
        final String signature;
        final String timestamp;

        Received(String body, String signature, String timestamp) {
            this.body = body;
            this.signature = signature;
            this.timestamp = timestamp;
        }
    }

    private HttpServer server;
    private final java.util.Queue<Received> received = new ConcurrentLinkedQueue<>();

    private void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            List<String> sig = exchange.getRequestHeaders().get("X-Buzhou-Signature");
            List<String> ts = exchange.getRequestHeaders().get("X-Buzhou-Timestamp");
            received.add(new Received(body,
                    sig == null ? null : sig.get(0), ts == null ? null : ts.get(0)));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
    }

    @Test
    void shouldRoundTripForwarderSignatureToVerifier() throws Exception {
        startServer();
        // 签名方：forwarder（secret 配置）经 outbox 真投递
        WebhookEventForwarder forwarder = new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                "whsec-round-trip", Duration.ofMillis(2000), 3, 64, null),
                new InMemorySessionStateStore());
        try (forwarder) {
            forwarder.onEvent(SessionEvent.of("test.verified", Map.of("k", "v")));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while (received.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
        }
        // 验签方：WebhookSignatures 容差窗——签名↔验签往返闭环
        assertThat(received).hasSize(1);
        Received request = received.poll();
        assertThat(request.signature).isNotBlank();
        assertThat(request.timestamp).isNotBlank();
        assertThat(WebhookSignatures.verify("whsec-round-trip", request.body,
                request.signature, request.timestamp,
                WebhookSignatures.DEFAULT_TOLERANCE, Instant.now())).isTrue();
        // 错 secret 的同请求验不过
        assertThat(WebhookSignatures.verify("wrong-secret", request.body,
                request.signature, request.timestamp,
                WebhookSignatures.DEFAULT_TOLERANCE, Instant.now())).isFalse();
    }
}
