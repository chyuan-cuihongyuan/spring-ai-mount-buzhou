package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-694 / spec 948：outbox 重试次数分布——首轮/退避轮分桶、TreeMap 升序、
 * 空表空分布、读面零行为变化。
 */
class RetryDistributionTest {

    @Test
    void bucketsByAttemptsAscending() {
        SessionStateStore store = new InMemorySessionStateStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 64);

        Instant now = Instant.now();
        // attempts=0 首投 2 条走 append
        for (int i = 0; i < 2; i++) {
            outbox.append("ev-first-" + i, "t", "p" + i);
        }
        // attempts=1/3 退避记录：手工构造 StateEntry（模拟 scheduleRetryOrDead 落盘形态）
        outbox.appendRetry(new WebhookOutbox.OutboxRecord("ev-retry-1", "t",
                "p", 100, 1, now.toEpochMilli() + 1000, now.toEpochMilli()));
        outbox.appendRetry(new WebhookOutbox.OutboxRecord("ev-deep-3a", "t",
                "p", 300, 3, now.toEpochMilli() + 2000, now.toEpochMilli()));
        outbox.appendRetry(new WebhookOutbox.OutboxRecord("ev-deep-3b", "t",
                "p", 301, 3, now.toEpochMilli() + 2001, now.toEpochMilli()));

        Map<Integer, Integer> dist = outbox.retryDistribution();
        assertThat(dist.get(0)).isEqualTo(2);
        assertThat(dist.get(1)).isEqualTo(1);
        assertThat(dist.get(3)).isEqualTo(2);
        // 升序键序（TreeMap 保证）
        assertThat(dist.keySet()).containsExactly(0, 1, 3);
    }

    @Test
    void emptyOutboxEmptyDistribution() {
        SessionStateStore store = new InMemorySessionStateStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 64);
        assertThat(outbox.retryDistribution()).isEmpty();
    }
}
