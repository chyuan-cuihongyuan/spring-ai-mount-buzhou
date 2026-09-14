package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spill 写速率限速测试（spec 1619 / T2389–T2390 / impl 1172）：令牌桶节流
 * （burst 内立即可写、超速等待）、软限速超时放行（degraded 计数）、关闭态零行为、
 * store 集成节流不破写入。RocksDB rate limiter 思想。
 */
class SpillWriteRateLimiterTest {

    @Test
    void burstAbsorbedImmediatelyThenThrottles() throws Exception {
        SpillWriteRateLimiter limiter = new SpillWriteRateLimiter(
                1000, 100, 5_000); // 1KB/s、burst 100B
        long start = System.nanoTime();
        assertThat(limiter.acquire(100)).isTrue(); // burst 内立即可写
        assertThat(Duration.ofNanos(System.nanoTime() - start).toMillis()).isLessThan(100);
        assertThat(limiter.acquire(100)).isTrue(); // 需等 ~100ms 补令牌
        long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        assertThat(elapsedMs).isGreaterThanOrEqualTo(50); // 节流发生（宽松下界防 CI 抖动）
        assertThat(limiter.throttledWaits()).isPositive();
    }

    @Test
    void maxWaitBypassesWithDegradedCount() throws Exception {
        SpillWriteRateLimiter limiter = new SpillWriteRateLimiter(1, 1, 50); // 1B/s、50ms 超时
        assertThat(limiter.acquire(1000)).isTrue(); // 远超速率——等待超时放行
        assertThat(limiter.degradedBypasses()).isEqualTo(1); // 软限速可观测
    }

    @Test
    void disabledLimiterAcquiresInstantly() throws Exception {
        SpillWriteRateLimiter limiter = new SpillWriteRateLimiter(0, 0, 0);
        assertThat(limiter.enabled()).isFalse();
        long start = System.nanoTime();
        assertThat(limiter.acquire(1_000_000)).isTrue();
        assertThat(Duration.ofNanos(System.nanoTime() - start).toMillis()).isLessThan(10);
    }

    @Test
    void storeIntegrationThrottlesWithoutBreakingWrite(@TempDir Path dir) throws Exception {
        SpillWriteRateLimiter limiter = new SpillWriteRateLimiter(10_000, 100, 100);
        DiskSpillStore store = new DiskSpillStore(dir, SpillQuota.unbounded(), null, limiter);
        SpillEntry entry = new SpillEntry(SpillUri.parse("spill://a/s/t1"),
                "x".repeat(200), "text/plain", 200, Instant.now());
        store.store(entry, 20); // burst 100 < 200 → 节流等待 → 超时放行写入成功
        assertThat(store.load(SpillUri.parse("spill://a/s/t1"))).hasValue("x".repeat(200));
        assertThat(limiter.degradedBypasses()).isPositive();
    }

    @Test
    void invalidConfigFailsFast() {
        assertThatThrownBy(() -> new SpillWriteRateLimiter(-1, 10, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
