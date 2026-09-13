package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-660 / spec 907：outbox 投递批量 AIMD 自适应——默认关恒 BATCH、
 * 全成功批加性增、失败批乘性减、下限夹取、读数面。
 */
class WebhookAimdBatchTest {

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

    private static final class Collector {
        final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();
        volatile int status = 200;
        volatile int failFirstN = 0;
        volatile int hits = 0;
    }

    private void startServer(Collector collector) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            collector.hits++;
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int status = collector.hits <= collector.failFirstN ? 500 : collector.status;
            if (status == 200) {
                collector.received.add(body);
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
    }

    private WebhookEventForwarder newForwarder(SessionStateStore store) {
        return new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                null, Duration.ofMillis(2000), 3, 100, null), store);
    }

    private static void await(java.util.function.BooleanSupplier condition)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("等待超时");
            }
            Thread.sleep(20);
        }
    }

    @Test
    void disabledKeepsBatchConstant() throws Exception {
        Collector collector = new Collector();
        startServer(collector);
        forwarder = newForwarder(new InMemorySessionStateStore());
        assertThat(forwarder.currentBatchSize()).isEqualTo(32); // BATCH 常量
        forwarder.onEvent(SessionEvent.of("e.one", Map.of("sessionId", "s1")));
        await(() -> forwarder.delivered() == 1);
        assertThat(forwarder.currentBatchSize()).isEqualTo(32); // 关闭态不增
    }

    @Test
    void allDeliveredBatchGrowsAdditively() throws Exception {
        Collector collector = new Collector();
        startServer(collector);
        forwarder = newForwarder(new InMemorySessionStateStore());
        forwarder.setAdaptiveBatchEnabled(true);

        // 3 条事件全部成功——批裁决 +1（批内事件数 < 批量也裁决：批完整语义 = due 非空且无失败无 defer）
        forwarder.onEvent(SessionEvent.of("e.a", Map.of("sessionId", "s1")));
        forwarder.onEvent(SessionEvent.of("e.b", Map.of("sessionId", "s1")));
        await(() -> forwarder.delivered() >= 2);
        await(() -> forwarder.currentBatchSize() >= 33);
        assertThat(forwarder.currentBatchSize()).isLessThanOrEqualTo(64);
    }

    @Test
    void failingBatchHalvesMultiplicatively() throws Exception {
        Collector collector = new Collector();
        collector.failFirstN = Integer.MAX_VALUE; // 恒 500（RETRYABLE）
        collector.status = 500;
        startServer(collector);
        forwarder = newForwarder(new InMemorySessionStateStore());
        forwarder.setAdaptiveBatchEnabled(true);

        int before = forwarder.currentBatchSize(); // 32
        forwarder.onEvent(SessionEvent.of("e.fail", Map.of("sessionId", "s1")));
        await(() -> forwarder.currentBatchSize() < before); // 乘性减 32→16
        assertThat(forwarder.currentBatchSize()).isEqualTo(16);
    }

    @Test
    void pureDecisionCoversClampsAndMixedBatches() {
        // 乘性减与下限夹取
        assertThat(WebhookEventForwarder.adjustedBatch(32, 0, 1, 0, 1)).isEqualTo(16);
        assertThat(WebhookEventForwarder.adjustedBatch(2, 0, 5, 0, 3)).isEqualTo(1);
        assertThat(WebhookEventForwarder.adjustedBatch(1, 0, 1, 0, 1)).isEqualTo(1); // 下限夹取
        // 加性增与上限夹取
        assertThat(WebhookEventForwarder.adjustedBatch(63, 10, 0, 0, 10)).isEqualTo(64);
        assertThat(WebhookEventForwarder.adjustedBatch(64, 10, 0, 0, 10)).isEqualTo(64);
        // defer 混合批：中性保持
        assertThat(WebhookEventForwarder.adjustedBatch(32, 5, 0, 3, 8)).isEqualTo(32);
        // 空批：不裁决
        assertThat(WebhookEventForwarder.adjustedBatch(32, 0, 0, 0, 0)).isEqualTo(32);
    }
}
