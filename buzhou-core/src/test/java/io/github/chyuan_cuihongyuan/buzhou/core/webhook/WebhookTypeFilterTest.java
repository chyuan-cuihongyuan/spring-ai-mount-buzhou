package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 105 §B / T388：webhook 订阅类型过滤红队——include-types 命中才入队投递
 * （被滤事件不占 outbox 容量）；空集 = 全投递零变化。借鉴：GitHub/Stripe webhook
 * 事件订阅面（消费端只收关心的类型）。
 */
class WebhookTypeFilterTest {

    private HttpServer server;
    private WebhookEventForwarder forwarder;
    private final ConcurrentLinkedQueue<String> types = new ConcurrentLinkedQueue<>();

    @AfterEach
    void tearDown() {
        if (forwarder != null) {
            forwarder.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    private void startCollector() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            try {
                types.add((String) new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(body, java.util.Map.class).get("type"));
            } catch (IOException parseFailure) {
                types.add("<unparseable>");
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
    }

    private WebhookEventForwarder forwarder(List<String> include) {
        WebhookEventForwarder f = new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                null, Duration.ofMillis(2000), 3, 100, null),
                new InMemorySessionStateStore());
        f.setIncludeTypes(include);
        return f;
    }

    @Test
    void onlyIncludedTypesAreEnqueuedAndDelivered() throws Exception {
        startCollector();
        forwarder = forwarder(List.of("user.turn.completed", "eval.run.completed"));

        forwarder.onEvent(SessionEvent.of("user.turn.completed", Map.of()));
        forwarder.onEvent(SessionEvent.of("tool.called", Map.of())); // 被滤
        forwarder.onEvent(SessionEvent.of("eval.run.completed", Map.of()));

        await(() -> types.size() == 2);
        Thread.sleep(200); // 若误投会有第三条
        assertThat(types).containsExactlyInAnyOrder("user.turn.completed", "eval.run.completed");
        assertThat(forwarder.pendingCount()).isZero(); // 被滤事件不占 outbox
    }

    @Test
    void emptyFilterDeliversEverythingUnchanged() throws Exception {
        startCollector();
        forwarder = forwarder(List.of()); // 空集 = 全投递（默认零变化）

        forwarder.onEvent(SessionEvent.of("tool.called", Map.of()));
        forwarder.onEvent(SessionEvent.of("memory.compacted", Map.of()));

        await(() -> types.size() == 2);
        assertThat(types).containsExactlyInAnyOrder("tool.called", "memory.compacted");
    }

    private static void await(java.util.function.BooleanSupplier condition)
            throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("await 超时");
            }
            Thread.sleep(20);
        }
    }
}
