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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 151 / T508：多 sink 扇出回归——类型路由（订阅 sink 只收订阅族）/ 全投
 * sink 两件全收 / 一 sink 拒收不影响别家 / close 后停发。双 HttpServer 收件。
 */
class WebhookFanoutTest {

    private final List<HttpServer> servers = new java.util.ArrayList<>();
    private final List<WebhookEventForwarder> forwarders = new java.util.ArrayList<>();

    @AfterEach
    void tearDown() {
        forwarders.forEach(WebhookEventForwarder::close);
        servers.forEach(s -> s.stop(0));
    }

    private static final class Collector {
        final ConcurrentLinkedQueue<String> bodies = new ConcurrentLinkedQueue<>();
        volatile int status = 200;
        final AtomicInteger hits = new AtomicInteger();
    }

    private HttpServer startServer(Collector collector) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            collector.hits.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);
            collector.bodies.add(body);
            byte[] resp = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(collector.status, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.start();
        servers.add(server);
        return server;
    }

    private WebhookEventForwarder newForwarder(HttpServer server) {
        WebhookEventForwarder forwarder = new WebhookEventForwarder(
                new BuzhouWebhookProperties(
                        "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                        null, Duration.ofMillis(2000), 2, 32, null),
                new InMemorySessionStateStore());
        forwarders.add(forwarder);
        return forwarder;
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
    void routesByTypeAcrossSinks() throws Exception {
        Collector audit = new Collector();
        Collector alarm = new Collector();
        WebhookEventForwarder auditSink = newForwarder(startServer(audit));
        WebhookEventForwarder alarmSink = newForwarder(startServer(alarm));
        alarmSink.setIncludeTypes(List.of("session.error"));

        try (WebhookFanout fanout = new WebhookFanout(List.of(auditSink, alarmSink))) {
            fanout.onEvent(SessionEvent.of("session.error", Map.of("sessionId", "s1")));
            fanout.onEvent(SessionEvent.of("user.turn.completed", Map.of("sessionId", "s1")));
        }

        await(() -> audit.bodies.size() >= 2);
        await(() -> alarm.bodies.size() >= 1);
        // 审计全量：两件都收
        assertThat(audit.bodies).hasSize(2);
        // 告警只收错误族：一件
        assertThat(alarm.bodies).hasSize(1);
        assertThat(alarm.bodies.peek()).contains("session.error");
    }

    @Test
    void oneSinkRejectingDoesNotAffectTheOther() throws Exception {
        Collector dead = new Collector();
        dead.status = 500; // 一直失败（进退避/死信）
        Collector healthy = new Collector();
        WebhookEventForwarder deadSink = newForwarder(startServer(dead));
        WebhookEventForwarder healthySink = newForwarder(startServer(healthy));

        try (WebhookFanout fanout = new WebhookFanout(List.of(deadSink, healthySink))) {
            fanout.onEvent(SessionEvent.of("user.turn.completed", Map.of("sessionId", "s2")));
        }

        await(() -> healthy.bodies.size() >= 1);
        assertThat(healthy.bodies).hasSize(1); // 死 sink 不拖累健康 sink
        assertThat(deadSink.pendingCount()).isGreaterThan(0); // 死 sink 在退避积压
    }

    @Test
    void noFanoutKeepsSingleForwarderBehaviorUnchanged() throws Exception {
        Collector single = new Collector();
        WebhookEventForwarder sink = newForwarder(startServer(single));

        sink.onEvent(SessionEvent.of("user.turn.completed", Map.of("sessionId", "s3")));

        await(() -> single.bodies.size() >= 1);
        assertThat(single.bodies).hasSize(1);
        assertThat(sink.delivered()).isEqualTo(1);
    }

    @Test
    void emptyFanoutDropsNothingAndCountsZero() {
        try (WebhookFanout fanout = new WebhookFanout(List.of())) {
            fanout.onEvent(SessionEvent.of("user.turn.completed", Map.of("sessionId", "s4")));
            assertThat(fanout.sinkCount()).isZero();
        }
    }
}
