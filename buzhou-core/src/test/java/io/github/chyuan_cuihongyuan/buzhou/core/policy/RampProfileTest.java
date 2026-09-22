package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1892 / T2986：阶梯加压——爬坡插值、保持、峰值、畸形。 */
class RampProfileTest {

    private static final List<RampProfile.Stage> PLAN = List.of(
            new RampProfile.Stage(50, 10_000),
            new RampProfile.Stage(20, 10_000),
            new RampProfile.Stage(20, 10_000));

    /** 三台阶五采样点：正爬/反爬/稳态/超时保持。 */
    @Test
    void rampInterpolatesLinearly() {
        assertThat(RampProfile.targetAt(PLAN, 5_000)).isCloseTo(25.0, within(1e-9));
        assertThat(RampProfile.targetAt(PLAN, 15_000)).isCloseTo(35.0, within(1e-9));
        assertThat(RampProfile.targetAt(PLAN, 20_000)).isCloseTo(20.0, within(1e-9));
        assertThat(RampProfile.targetAt(PLAN, 25_000)).isCloseTo(20.0, within(1e-9));
        assertThat(RampProfile.targetAt(PLAN, 35_000)).isCloseTo(20.0, within(1e-9));
    }

    /** 台阶边界恰值：10s 处 = 50（首台阶末）、20s 处 = 20（次台阶末）。 */
    @Test
    void stageBoundariesExact() {
        assertThat(RampProfile.targetAt(PLAN, 10_000)).isCloseTo(50.0, within(1e-9));
        assertThat(RampProfile.targetAt(PLAN, 0)).isCloseTo(0.0, within(1e-9));
    }

    /** 峰值与总时长声明。 */
    @Test
    void peakAndTotalDeclaration() {
        assertThat(RampProfile.peakTarget(PLAN)).isEqualTo(50);
        assertThat(RampProfile.totalDuration(PLAN)).isEqualTo(30_000);
    }

    /** 畸形入参 fail-fast：空表、零时长台阶、负目标、负时刻。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> RampProfile.targetAt(List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("台阶表不能为空");
        assertThatThrownBy(() -> RampProfile.targetAt(
                List.of(new RampProfile.Stage(10, 0)), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("台阶时长不能小于 1");
        assertThatThrownBy(() -> RampProfile.targetAt(
                List.of(new RampProfile.Stage(-1, 1_000)), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("台阶目标不能为负");
        assertThatThrownBy(() -> RampProfile.targetAt(PLAN, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("elapsed 不能为负");
    }
}
