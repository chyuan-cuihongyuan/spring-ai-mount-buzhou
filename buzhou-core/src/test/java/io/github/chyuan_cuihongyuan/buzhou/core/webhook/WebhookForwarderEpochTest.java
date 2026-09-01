package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 303 / impl-326：发送侧持久纪元端到端——首启信封带 epoch E1>0 且 seq 从 1；
 * 同 store 重建 forwarder（模拟重启）→ epoch E2>E1（持久递增不撞号）。
 * JDK HttpServer 收件，对齐 WebhookEventForwarderTest 手法。
 */
class WebhookForwarderEpochTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpServer server;
    private final ConcurrentLinkedQueue<JsonNode> received = new ConcurrentLinkedQueue<>();
    private WebhookEventForwarder forwarder;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            received.add(MAPPER.readTree(body));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        if (forwarder != null) {
            forwarder.close();
        }
        server.stop(0);
    }

    private WebhookEventForwarder newForwarder(SessionStateStore store) {
        return new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                "secret", Duration.ofMillis(2000), 2, 64, null), store);
    }

    private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("等待超时");
            }
            Thread.sleep(20);
        }
    }

    @Test
    void restartAdvancesPersistedEpoch() throws Exception {
        SessionStateStore store = Buzhou.inMemoryStores().sessionStateStore();

        forwarder = newForwarder(store);
        forwarder.onEvent(new io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent(
                "turn.completed", java.util.Map.of(), java.time.Instant.now()));
        await(() -> received.size() >= 1);
        JsonNode first = received.peek();
        long epoch1 = first.get("epoch").asLong();
        assertThat(epoch1).as("首启纪元恒正").isPositive();
        assertThat(first.get("seq").asLong()).isEqualTo(1L);
        forwarder.close();

        forwarder = newForwarder(store); // 模拟重启：同 store 重建
        forwarder.onEvent(new io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent(
                "turn.completed", java.util.Map.of(), java.time.Instant.now()));
        await(() -> received.size() >= 2);
        List<JsonNode> all = List.copyOf(received);
        JsonNode second = all.get(all.size() - 1);
        assertThat(second.get("epoch").asLong())
                .as("重启纪元持久递增").isGreaterThan(epoch1);
        assertThat(second.get("seq").asLong())
                .as("新纪元 seq 从 1 重新起算").isEqualTo(1L);
    }
}
