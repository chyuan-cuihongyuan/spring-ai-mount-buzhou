package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 514 / T777–T778：投递时延分位数——exact 最近秩 p50/p95/p99、零样本
 * null 诚实空值、窗口有界、负值忽略、forwarder 接线（DELIVERED 缝记样本）。
 */
class WebhookDeliveryLatencyTest {

    @Test
    void percentilesExactNearestRank() {
        WebhookDeliveryLatency latency = new WebhookDeliveryLatency();
        for (long i = 1; i <= 100; i++) {
            latency.record(i); // 样本 1..100 ms
        }
        var snap = latency.snapshot();
        assertThat(snap.deliveredCount()).isEqualTo(100);
        assertThat(snap.p50Millis()).isEqualTo(50);
        assertThat(snap.p95Millis()).isEqualTo(95);
        assertThat(snap.p99Millis()).isEqualTo(99);
        assertThat(snap.maxMillis()).isEqualTo(100);
    }

    @Test
    void emptySnapshotHasNullPercentilesNotZeros() {
        var snap = new WebhookDeliveryLatency().snapshot();
        assertThat(snap.deliveredCount()).isZero();
        assertThat(snap.p50Millis()).isNull();
        assertThat(snap.p95Millis()).isNull();
        assertThat(snap.p99Millis()).isNull();
    }

    @Test
    void windowIsBoundedAndNegativeIgnored() {
        WebhookDeliveryLatency latency = new WebhookDeliveryLatency(16);
        for (long i = 1; i <= 40; i++) {
            latency.record(i);
        }
        latency.record(-5); // 时钟回拨防御——忽略
        assertThat(latency.sampleCount()).isEqualTo(16);
        // 窗内只剩 25..40：p50 = 25+ceil(0.5*16)-1 = 32
        assertThat(latency.snapshot().p50Millis()).isEqualTo(32);
    }

    @Test
    void forwarderFeedsLatencyOnSuccessfulDelivery() throws Exception {
        // 本地 HTTP 收端（webhook 族测试同款 harness）
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        var hits = new java.util.concurrent.atomic.AtomicInteger();
        server.createContext("/hook", exchange -> {
            byte[] body = "ok".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
            hits.incrementAndGet();
        });
        server.start();
        try {
            var props = new io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                    null, null, null, null, null);
            var stores = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores();
            WebhookDeliveryLatency latency = new WebhookDeliveryLatency();
            var forwarder = new WebhookEventForwarder(props, stores.sessionStateStore());
            forwarder.setDeliveryLatency(latency);
            forwarder.onEvent(new io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent(
                    "session.updated", Map.of("k", "v"), Instant.now()));
            // 等投递（dispatcher 周期）
            for (int i = 0; i < 50 && latency.sampleCount() == 0; i++) {
                Thread.sleep(100);
            }
            forwarder.close();
            server.stop(0);
            assertThat(latency.sampleCount()).isEqualTo(1);
            assertThat(latency.snapshot().p50Millis()).isGreaterThanOrEqualTo(0);
        } finally {
            server.stop(0);
        }
    }
}
