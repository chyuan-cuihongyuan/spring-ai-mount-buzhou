package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * impl-673 / spec 920：限流器余量快照——满桶起步、消耗递减、refill 时点修正、
 * deferred 透传、tryAcquire 既有语义零变化。
 */
class RatelimitSnapshotTest {

    @Test
    void snapshotReflectsCapacityAndConsumption() {
        AtomicLong clock = new AtomicLong(1_000_000);
        WebhookRateLimiter limiter = new WebhookRateLimiter(5, 0.5, clock::get);

        // 满桶起步
        WebhookRateLimiter.Snapshot full = limiter.snapshot();
        assertThat(full.tokens()).isCloseTo(5.0, within(1e-9));
        assertThat(full.capacity()).isEqualTo(5.0);
        assertThat(full.refillPerSecond()).isEqualTo(0.5);
        assertThat(full.deferred()).isZero();

        // 消耗 2 枚 → 余量 3
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.snapshot().tokens()).isCloseTo(3.0, within(1e-9));
    }

    @Test
    void deferredCountFlowsThroughSnapshot() {
        AtomicLong clock = new AtomicLong(1_000_000);
        WebhookRateLimiter limiter = new WebhookRateLimiter(1, 0.001, clock::get);
        // 桶容量 1：连续 3 次取令牌，第 2/3 次 defer
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
        assertThat(limiter.tryAcquire()).isFalse();

        WebhookRateLimiter.Snapshot snapshot = limiter.snapshot();
        assertThat(snapshot.deferred()).isEqualTo(2);
        assertThat(snapshot.tokens()).isLessThan(1.0);
    }

    @Test
    void refillReflectedInSnapshotWithClockAdvance() {
        AtomicLong clock = new AtomicLong(1_000_000);
        WebhookRateLimiter limiter = new WebhookRateLimiter(2, 1.0, clock::get);
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse(); // 空桶

        clock.addAndGet(2000); // 前进 2 秒：refill 2 枚（封顶 capacity=2）
        WebhookRateLimiter.Snapshot snapshot = limiter.snapshot();
        assertThat(snapshot.tokens()).isCloseTo(2.0, within(1e-9)); // 回满（封顶夹取）
    }

    @Test
    void tryAcquireSemanticsUnchanged() {
        AtomicLong clock = new AtomicLong(1_000_000);
        WebhookRateLimiter limiter = new WebhookRateLimiter(2, 0.001, clock::get);
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse(); // refillPerSecond=0 不回填
        assertThat(limiter.deferredCount()).isEqualTo(1);
    }
}
