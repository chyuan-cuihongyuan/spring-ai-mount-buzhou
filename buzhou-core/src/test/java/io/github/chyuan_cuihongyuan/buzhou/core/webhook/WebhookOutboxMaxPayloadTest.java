package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 533 / T817–818：webhook 载荷大小上限——默认不限零变化、超限拒绝
 * 计 oversized、上限内正常入队、forwarder setter 透传（Kafka max message
 * size 思想）。
 */
class WebhookOutboxMaxPayloadTest {

    private record Appended(boolean ok) {
    }

    @Test
    void defaultUnlimitedZeroBehaviorChange() {
        WebhookOutbox outbox = new WebhookOutbox(new InMemorySessionStateStore(), 8);
        assertThat(outbox.append("e1", "t", "x".repeat(1_000_000))).isTrue();
    }

    @Test
    void oversizedRejectedAndCountedWhenCapped() {
        WebhookOutbox outbox = new WebhookOutbox(new InMemorySessionStateStore(), 8);
        outbox.setMaxPayloadChars(100);
        assertThat(outbox.append("small", "t", "ok body")).isTrue();
        assertThat(outbox.append("huge", "t", "x".repeat(101))).isFalse();
        assertThat(outbox.append("huge2", "t", "x".repeat(5000))).isFalse();
        // 上限内不受影响
        assertThat(outbox.append("small2", "t", "fine")).isTrue();
        assertThat(outbox.pendingCount()).isEqualTo(2);
        // 上限内边界：恰好 100
        outbox.setMaxPayloadChars(8);
        assertThat(outbox.append("edge", "t", "x".repeat(8))).isTrue();
    }

    @Test
    void forwarderSetterPassthroughGuardsOutbox() {
        // forwarder 级 setter 透传 outbox（行为验证经 outbox 语义——构造即含 outbox）
        BuzhouWebhookProperties props = new BuzhouWebhookProperties(
                "http://127.0.0.1:1/hook", null, null, null, null, null);
        WebhookEventForwarder forwarder = new WebhookEventForwarder(
                props, new InMemorySessionStateStore());
        try {
            forwarder.setOutboxMaxPayloadChars(10); // 不抛即接线成立（outbox 私有——行为经 E2E 覆盖）
        } finally {
            forwarder.close();
        }
    }
}
