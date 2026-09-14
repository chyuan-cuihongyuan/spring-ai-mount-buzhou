package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1403 / T2108：EWMA 自适应超时推荐器——EWMA 数学、预热哨兵、
 * 夹取边界、放大倍数覆盖长尾、reset；推荐值从实测推导（纯推导器不接线）。
 */
class AdaptiveTimeoutTest {

    @Test
    void warmupPeriodReturnsEmptyUntilMinSamples() {
        AdaptiveTimeout timeout = new AdaptiveTimeout();
        assertThat(timeout.recommended()).isEmpty();
        timeout.record(100);
        timeout.record(100);
        assertThat(timeout.recommended()).isEmpty(); // 2 < MIN_SAMPLES=3
        timeout.record(100);
        assertThat(timeout.recommended()).contains(Duration.ofMillis(300)); // 100×3
    }

    @Test
    void ewmaWeightsRecentSamplesAndForgetsHistory() {
        AdaptiveTimeout timeout = new AdaptiveTimeout(
                AdaptiveTimeout.DEFAULT_ALPHA, AdaptiveTimeout.DEFAULT_MULTIPLIER,
                Duration.ofMillis(10), Duration.ofSeconds(60));
        // 1s×2 播种后突增 10s×3：EWMA 应快速上移（新样本权重 0.3）
        timeout.record(1000);
        timeout.record(1000);
        timeout.record(10_000);
        long afterOne = timeout.stats().ewmaMillis(); // 0.3×10000+0.7×1000=3700
        timeout.record(10_000);
        timeout.record(10_000);
        long afterThree = timeout.stats().ewmaMillis(); // ≈ 0.3×10³+0.7×3700=5590
        assertThat(afterOne).isEqualTo(3700);
        assertThat(afterThree).isGreaterThan(afterOne);
        // 推荐值随 EWMA 上移而放大（覆盖长尾）
        assertThat(timeout.recommended()).contains(Duration.ofMillis(
                (long) Math.ceil(afterThree * 3)));
    }

    @Test
    void recommendationIsClampedToFloorAndCeiling() {
        AdaptiveTimeout timeout = new AdaptiveTimeout(0.9, 5,
                Duration.ofMillis(500), Duration.ofMillis(2000));
        timeout.record(1);
        timeout.record(1);
        timeout.record(1);
        // EWMA=1ms ×5 = 5ms → 下限夹取 500ms
        assertThat(timeout.recommended()).contains(Duration.ofMillis(500));
        timeout.record(10_000);
        timeout.record(10_000);
        timeout.record(10_000);
        // EWMA≈9727×5 远超上限 → 夹取 2000ms
        assertThat(timeout.recommended()).contains(Duration.ofMillis(2000));
    }

    @Test
    void recoveryLowersRecommendationGradually() {
        AdaptiveTimeout timeout = new AdaptiveTimeout(0.5, 2,
                Duration.ofMillis(10), Duration.ofSeconds(60));
        timeout.record(1000);
        timeout.record(1000);
        timeout.record(1000);
        long high = timeout.recommended().orElseThrow().toMillis(); // 2000
        timeout.record(100);
        timeout.record(100);
        timeout.record(100);
        // EWMA=0.5³ 遗忘链：e₃=212.5，×2=425（历史权重可见——不是回落到 100×2）
        long low = timeout.recommended().orElseThrow().toMillis();
        assertThat(high).isEqualTo(2000);
        assertThat(low).isEqualTo(425);
        assertThat(low).isLessThan(high);
    }

    @Test
    void resetForTestClearsEwmaAndCounters() {
        AdaptiveTimeout timeout = new AdaptiveTimeout();
        timeout.record(100);
        timeout.record(100);
        timeout.record(100);
        assertThat(timeout.stats().samples()).isEqualTo(3);
        timeout.resetForTest();
        assertThat(timeout.stats().samples()).isZero();
        assertThat(timeout.stats().ewmaMillis()).isEqualTo(-1);
        assertThat(timeout.recommended()).isEmpty();
    }

    @Test
    void invalidParametersAndSamplesAreRejected() {
        assertThatThrownBy(() -> new AdaptiveTimeout(0, 3, Duration.ZERO, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AdaptiveTimeout(0.5, 0, Duration.ZERO, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AdaptiveTimeout(0.5, 3,
                Duration.ofSeconds(2), Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        AdaptiveTimeout timeout = new AdaptiveTimeout();
        assertThatThrownBy(() -> timeout.record(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void statsTracksRecommendationCount() {
        AdaptiveTimeout timeout = new AdaptiveTimeout();
        assertThat(timeout.stats().recommendationCount()).isZero();
        for (int i = 0; i < 5; i++) {
            timeout.record(50);
        }
        timeout.recommended();
        timeout.recommended();
        assertThat(timeout.stats().recommendationCount()).isEqualTo(2);
        assertThat(timeout.stats().multiplier())
                .isEqualTo(AdaptiveTimeout.DEFAULT_MULTIPLIER);
        assertThat(timeout.stats().samples()).isEqualTo(5);
    }
}
