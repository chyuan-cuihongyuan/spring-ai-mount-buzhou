package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 145 / T500：自适应并发回归——加性增封顶 / 积性减地板 / 动态上限拒 /
 * Lease 归还 / 隔离与计数。
 */
class AdaptiveBulkheadTest {

    /** 快测配置：initial 2 / max 4 / 每 2 连续成功 +1。 */
    private static AdaptiveBulkhead.Config fast() {
        return new AdaptiveBulkhead.Config(2, 4, 2);
    }

    @Test
    void additiveIncreaseUpToMax() {
        AdaptiveBulkhead bulkhead = new AdaptiveBulkhead(fast());
        assertThat(bulkhead.limitOf("a")).isEqualTo(2);
        bulkhead.recordSuccess("a");
        assertThat(bulkhead.limitOf("a")).isEqualTo(2); // 未满步进
        bulkhead.recordSuccess("a");
        assertThat(bulkhead.limitOf("a")).isEqualTo(3); // +1
        bulkhead.recordSuccess("a");
        bulkhead.recordSuccess("a");
        assertThat(bulkhead.limitOf("a")).isEqualTo(4); // +1 到顶
        bulkhead.recordSuccess("a");
        bulkhead.recordSuccess("a");
        assertThat(bulkhead.limitOf("a")).isEqualTo(4); // 封顶不越
    }

    @Test
    void multiplicativeDecreaseFlooredAtOne() {
        AdaptiveBulkhead bulkhead = new AdaptiveBulkhead(fast());
        bulkhead.recordSuccess("a");
        bulkhead.recordSuccess("a"); // limit 3
        bulkhead.recordFailure("a");
        assertThat(bulkhead.limitOf("a")).isEqualTo(1); // 减半地板
        bulkhead.recordFailure("a");
        assertThat(bulkhead.limitOf("a")).isEqualTo(1); // 已在地板不再降
    }

    @Test
    void acquireRespectsDynamicLimitAndFailsFast() {
        AdaptiveBulkhead bulkhead = new AdaptiveBulkhead(fast());
        try (AdaptiveBulkhead.Lease one = bulkhead.acquire("a");
             AdaptiveBulkhead.Lease two = bulkhead.acquire("a")) {
            // 动态上限 2 已满 → 第 3 个 fail-fast（QUOTA_EXCEEDED 同词汇）
            assertThatThrownBy(() -> bulkhead.acquire("a"))
                    .isInstanceOf(BuzhouException.class)
                    .hasMessageContaining("自适应并发上限")
                    .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                            .isEqualTo(ErrorCode.QUOTA_EXCEEDED));
        }
        // Lease 归还后可再取
        try (AdaptiveBulkhead.Lease again = bulkhead.acquire("a")) {
            assertThat(bulkhead.snapshot().get("a").inFlight()).isEqualTo(1);
        }
        assertThat(bulkhead.snapshot().get("a").blocked()).isEqualTo(1);
    }

    @Test
    void decreaseTakesEffectOnAcquireImmediately() {
        AdaptiveBulkhead bulkhead = new AdaptiveBulkhead(fast());
        bulkhead.recordFailure("a"); // limit 2 → 1
        try (AdaptiveBulkhead.Lease only = bulkhead.acquire("a")) {
            assertThatThrownBy(() -> bulkhead.acquire("a"))
                    .isInstanceOf(BuzhouException.class);
        }
    }

    @Test
    void agentsAreIsolated() {
        AdaptiveBulkhead bulkhead = new AdaptiveBulkhead(fast());
        bulkhead.recordFailure("a"); // a 降到 1
        assertThat(bulkhead.limitOf("b")).isEqualTo(2); // b 不受影响
        bulkhead.recordSuccess("b");
        bulkhead.recordSuccess("b");
        assertThat(bulkhead.limitOf("b")).isEqualTo(3);
        assertThat(bulkhead.snapshot().keySet()).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void configValidatedAndAdjustmentCounted() {
        assertThatThrownBy(() -> new AdaptiveBulkhead.Config(0, 4, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AdaptiveBulkhead.Config(4, 2, 2))
                .isInstanceOf(IllegalArgumentException.class);

        AdaptiveBulkhead bulkhead = new AdaptiveBulkhead(fast());
        bulkhead.recordSuccess("a");
        bulkhead.recordSuccess("a"); // 一次上调
        bulkhead.recordFailure("a"); // 一次下调
        assertThat(bulkhead.totalAdjustments()).isEqualTo(2);
        assertThat(bulkhead.snapshot().get("a").adjustments()).isEqualTo(2);
    }
}
