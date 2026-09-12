package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 死信重放审计事件测试（spec 721 / T993–T994 / impl 524）：重放指标 delta、
 * 累计计数、零重放零事件。审计面直接驱动（outbox 同 store 视图造死信——
 * 不经 HTTP 管线）。
 */
class WebhookDeadReplayAuditTest {

    static final class CapturingMetrics implements BuzhouMetrics {
        final ConcurrentLinkedQueue<String> events = new ConcurrentLinkedQueue<>();

        @Override
        public void counter(String name, long delta, String... tagKeyValue) {
            events.add(name + "x" + delta);
        }

        @Override
        public void timer(String name, Duration duration, String... tagKeyValue) {
            events.add(name);
        }
    }

    private final CapturingMetrics metrics = new CapturingMetrics();

    @AfterEach
    void tearDown() {
        BuzhouMetricsHolder.reset();
    }

    private static WebhookEventForwarder forwarderOn(SessionStateStore store) {
        return new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:1/hook", "test-secret", Duration.ofMillis(200),
                3, 64, null), store);
    }

    @Test
    void replayEmitsMetricAndCumulativeCounters() {
        BuzhouMetricsHolder.install(metrics);
        SessionStateStore store = new InMemorySessionStateStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 64); // 同 store 视图
        for (int i = 0; i < 3; i++) {
            outbox.append("evt-" + i, "session.ended", "{\"i\":" + i + "}");
        }
        for (WebhookOutbox.OutboxRecord record : outbox.due(Instant.now(), 10)) {
            outbox.markDead(record); // 直接判死（重放对象的既有来源）
        }
        WebhookEventForwarder forwarder = forwarderOn(store);

        int requeued = forwarder.replayDeadLetters();

        assertThat(requeued).isEqualTo(3);
        assertThat(metrics.events).contains("buzhou.webhook.dead-replayedx3");
        assertThat(forwarder.replayCount()).isEqualTo(1);
        assertThat(forwarder.replayedCount()).isEqualTo(3);
        assertThat(outbox.deadLetters(10)).isEmpty(); // 死信已迁回 outbox
    }

    @Test
    void zeroReplayEmitsNothing() {
        BuzhouMetricsHolder.install(metrics);
        WebhookEventForwarder forwarder = forwarderOn(new InMemorySessionStateStore());

        int requeued = forwarder.replayDeadLetters();

        assertThat(requeued).isZero();
        assertThat(metrics.events).noneMatch(e -> e.startsWith("buzhou.webhook.dead-replayed"));
        assertThat(forwarder.replayCount()).isZero();
        assertThat(forwarder.replayedCount()).isZero();
    }

    @Test
    void repeatedReplaysAccumulate() {
        BuzhouMetricsHolder.install(metrics);
        SessionStateStore store = new InMemorySessionStateStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 64);
        WebhookEventForwarder forwarder = forwarderOn(store);

        outbox.append("evt-a", "session.ended", "{}");
        outbox.markDead(outbox.due(Instant.now(), 1).get(0));
        assertThat(forwarder.replayDeadLetters()).isEqualTo(1);

        outbox.append("evt-b", "session.ended", "{}");
        outbox.markDead(outbox.due(Instant.now(), 1).get(0));
        assertThat(forwarder.replayDeadLetters()).isEqualTo(1);

        assertThat(forwarder.replayCount()).isEqualTo(2);
        assertThat(forwarder.replayedCount()).isEqualTo(2);
    }
}
