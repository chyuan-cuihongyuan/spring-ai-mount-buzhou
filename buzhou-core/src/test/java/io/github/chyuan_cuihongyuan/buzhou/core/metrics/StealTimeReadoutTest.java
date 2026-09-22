package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1887 / T2976：窃取时间——占比、阈值判定、哨兵、畸形。 */
class StealTimeReadoutTest {

    /** 经典：Δsteal 60 / Δtotal 1000 = 6%。 */
    @Test
    void classicRatio() {
        assertThat(StealTimeReadout.stealRatio(10, 70, 1000, 2000))
                .isCloseTo(0.06, within(1e-12));
    }

    /** 阈值两侧：6% 对 5% 判争用、对 10% 判健康；恰等阈值即争用。 */
    @Test
    void thresholdSides() {
        assertThat(StealTimeReadout.isContended(0.06, 0.05)).isTrue();
        assertThat(StealTimeReadout.isContended(0.06, 0.10)).isFalse();
        assertThat(StealTimeReadout.isContended(0.05, 0.05)).isTrue();
    }

    /** 零流逝哨兵：Δtotal=0 → 0.0（诚实无信号）。 */
    @Test
    void zeroElapsedSentinel() {
        assertThat(StealTimeReadout.stealRatio(10, 10, 1000, 1000))
                .isCloseTo(0.0, within(1e-12));
    }

    /** 畸形入参 fail-fast：steal/total 倒退、阈值越界。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> StealTimeReadout.stealRatio(70, 10, 1000, 2000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("steal 计数倒退");
        assertThatThrownBy(() -> StealTimeReadout.stealRatio(10, 70, 2000, 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("total 计数倒退");
        assertThatThrownBy(() -> StealTimeReadout.isContended(0.5, 1.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threshold 须在 [0,1]");
    }
}
