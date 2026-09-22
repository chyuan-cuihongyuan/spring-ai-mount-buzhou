package io.github.chyuan_cuihongyuan.buzhou.core.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1884 / T2970：滑窗计数——插值、准入判定、畸形 fail-fast。 */
class SlidingWindowCounterTest {

    /** 插值四例：窗初惯性/窗末本窗/本窗全计（保守）/双百叠加。 */
    @Test
    void interpolationCases() {
        assertThat(SlidingWindowCounter.estimate(100, 0, 0.0)).isCloseTo(100.0, within(1e-12));
        assertThat(SlidingWindowCounter.estimate(100, 0, 1.0)).isCloseTo(0.0, within(1e-12));
        // Cloudflare 保守口径：curr 已发生的计数全计，prev 按剩余权衰减
        assertThat(SlidingWindowCounter.estimate(0, 100, 0.5)).isCloseTo(100.0, within(1e-12));
        assertThat(SlidingWindowCounter.estimate(100, 100, 0.5)).isCloseTo(150.0, within(1e-12));
    }

    /** 准入：估计 = limit 拒（满额语义）、< limit 放。 */
    @Test
    void admissionBoundary() {
        assertThat(SlidingWindowCounter.wouldExceed(100, 50, 0.5, 100)).isTrue();
        assertThat(SlidingWindowCounter.wouldExceed(100, 49, 0.5, 100)).isFalse();
        assertThat(SlidingWindowCounter.wouldExceed(0, 99, 0.9, 100)).isFalse();
    }

    /** 零流量窗估计恒 0；空窗起步正常。 */
    @Test
    void zeroTrafficStaysZero() {
        assertThat(SlidingWindowCounter.estimate(0, 0, 0.7)).isCloseTo(0.0, within(1e-12));
        assertThat(SlidingWindowCounter.wouldExceed(0, 0, 0.7, 1)).isFalse();
    }

    /** 畸形入参 fail-fast：负计数、ratio 越界、负限值。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> SlidingWindowCounter.estimate(-1, 0, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("窗口计数不能为负");
        assertThatThrownBy(() -> SlidingWindowCounter.estimate(0, 0, 1.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("elapsedRatio 须在 [0,1]");
        assertThatThrownBy(() -> SlidingWindowCounter.wouldExceed(0, 0, 0.5, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit 不能为负");
    }
}
