package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 159 / T518：投递序号围栏回归——信封 seq 单调（HTTP 收件断言）+ fence
 * 四裁决（连续/缺口/重复/复位）+ 无 seq 兼容放行 + 订阅流隔离。
 */
class SequenceFenceTest {

    private HttpServer server;
    private WebhookEventForwarder forwarder;
    private final ConcurrentLinkedQueue<String> bodies = new ConcurrentLinkedQueue<>();

    @AfterEach
    void tearDown() {
        if (forwarder != null) {
            forwarder.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
    }

    @Test
    void envelopeCarriesMonotonicSeq() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            byte[] resp = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.start();
        forwarder = new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                null, Duration.ofMillis(2000), 2, 32, null),
                new InMemorySessionStateStore());

        forwarder.onEvent(SessionEvent.of("user.turn.completed", Map.of("sessionId", "s1")));
        forwarder.onEvent(SessionEvent.of("user.turn.completed", Map.of("sessionId", "s1")));
        forwarder.onEvent(SessionEvent.of("user.turn.completed", Map.of("sessionId", "s1")));

        await(() -> bodies.size() >= 3);
        long first = seqOf(bodies.poll());
        long second = seqOf(bodies.poll());
        long third = seqOf(bodies.poll());
        assertThat(second).isEqualTo(first + 1);
        assertThat(third).isEqualTo(second + 1);
    }

    private static long seqOf(String body) {
        int idx = body.indexOf("\"seq\":");
        assertThat(idx).isGreaterThan(-1);
        int start = idx + "\"seq\":".length();
        int end = start;
        while (end < body.length() && Character.isDigit(body.charAt(end))) {
            end++;
        }
        return Long.parseLong(body.substring(start, end));
    }

    @Test
    void fenceVerdictsForAllFourCases() {
        SequenceFence fence = new SequenceFence();
        assertThat(fence.observe("sub", 10L)).isEqualTo(SequenceFence.Verdict.CONTINUE); // 首见建基线
        assertThat(fence.observe("sub", 11L)).isEqualTo(SequenceFence.Verdict.CONTINUE); // 连续
        assertThat(fence.observe("sub", 13L)).isEqualTo(SequenceFence.Verdict.GAP);      // 跳号——缺 12
        assertThat(fence.observe("sub", 13L)).isEqualTo(SequenceFence.Verdict.DUPLICATE); // 重投
        assertThat(fence.observe("sub", 14L)).isEqualTo(SequenceFence.Verdict.CONTINUE);
        assertThat(fence.observe("sub", 2L)).isEqualTo(SequenceFence.Verdict.RESET);     // 发送方重启
        assertThat(fence.observe("sub", 3L)).isEqualTo(SequenceFence.Verdict.CONTINUE); // 新纪元连续
    }

    @Test
    void legacyEnvelopeWithoutSeqPassesThrough() {
        SequenceFence fence = new SequenceFence();
        assertThat(fence.observe("sub", null)).isEqualTo(SequenceFence.Verdict.CONTINUE);
        // null 不建基线——后续带 seq 仍按首见处理
        assertThat(fence.observe("sub", 5L)).isEqualTo(SequenceFence.Verdict.CONTINUE);
    }

    @Test
    void subscriptionsAreIsolated() {
        SequenceFence fence = new SequenceFence();
        fence.observe("audit", 100L);
        assertThat(fence.observe("alarm", 1L)).isEqualTo(SequenceFence.Verdict.CONTINUE); // 独立基线
        assertThat(fence.observe("audit", 101L)).isEqualTo(SequenceFence.Verdict.CONTINUE);
    }
}
