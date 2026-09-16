package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 2044 / T3190：预热斜坡合同——起点恰 startFactor、线性爬升中点、
 * 预热期满恒 1、单调性、回拨宽进、畸形 fail-fast。
 */
class WarmupRampTest {

    @Test
    void startOfWarmupShouldYieldStartFactor() {
        WarmupRamp ramp = new WarmupRamp(10_000L, 0.1d);
        assertThat(ramp.factorAt(0)).isCloseTo(0.1d, within(1e-12)); // 恰起点
    }

    @Test
    void midpointShouldClimbLinearly() {
        WarmupRamp ramp = new WarmupRamp(10_000L, 0.1d);
        assertThat(ramp.factorAt(5_000)).isCloseTo(0.55d, within(1e-12)); // (0.1+1)/2
        assertThat(ramp.factorAt(2_500)).isCloseTo(0.325d, within(1e-12));
    }

    @Test
    void warmupEndShouldReachAndHoldFullSpeed() {
        WarmupRamp ramp = new WarmupRamp(10_000L, 0.1d);
        assertThat(ramp.factorAt(10_000)).isEqualTo(1.0d);  // 恰期满
        assertThat(ramp.factorAt(999_999)).isEqualTo(1.0d); // 恒满速
        assertThat(ramp.warmedUp(10_000)).isTrue();
        assertThat(ramp.warmedUp(9_999)).isFalse();
    }

    @Test
    void factorShouldBeMonotonicNonDecreasing() {
        WarmupRamp ramp = new WarmupRamp(1_000L, 0.2d);
        double prev = 0;
        for (long t = 0; t <= 1_200; t += 50) {
            double f = ramp.factorAt(t);
            assertThat(f).isGreaterThanOrEqualTo(prev);
            prev = f;
        }
    }

    @Test
    void clockRollbackShouldLenientToStart() {
        WarmupRamp ramp = new WarmupRamp(1_000L, 0.5d);
        assertThat(ramp.factorAt(-100)).isCloseTo(0.5d, within(1e-12)); // 宽进按起点
    }

    @Test
    void readoutsShouldEchoConstruction() {
        WarmupRamp ramp = new WarmupRamp(5_000L, 0.25d);
        assertThat(ramp.warmupMillis()).isEqualTo(5_000L);
        assertThat(ramp.startFactor()).isEqualTo(0.25d);
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new WarmupRamp(0, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WarmupRamp(1_000, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WarmupRamp(1_000, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
