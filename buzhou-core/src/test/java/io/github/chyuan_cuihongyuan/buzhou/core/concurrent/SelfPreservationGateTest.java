package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1885 / T2972：自保门——触发、恢复、边界、畸形。 */
class SelfPreservationGateTest {

    /** 低比率停逐 / 达标续逐两态（期望 100，阈值 0.15 → 15 触发线）。 */
    @Test
    void lowRatioStopsExpiration() {
        SelfPreservationGate gate = new SelfPreservationGate(100, 0.15);
        gate.resetWindow(10);
        assertThat(gate.selfPreserving()).isTrue();
        assertThat(gate.shouldExpire()).isFalse();
        gate.resetWindow(50);
        assertThat(gate.selfPreserving()).isFalse();
        assertThat(gate.shouldExpire()).isTrue();
    }

    /** 边界：续约数恰等于触发线（= 期望×阈值）按正常逐处理。 */
    @Test
    void boundaryIsNormalExpiration() {
        SelfPreservationGate gate = new SelfPreservationGate(100, 0.15);
        gate.resetWindow(15);
        assertThat(gate.selfPreserving()).isFalse();
        assertThat(gate.shouldExpire()).isTrue();
    }

    /** 续约计数与比率读数。 */
    @Test
    void renewalCountingAndRatio() {
        SelfPreservationGate gate = new SelfPreservationGate(40, 0.5);
        for (int i = 0; i < 30; i++) {
            gate.onRenewal();
        }
        assertThat(gate.renewalRatio()).isCloseTo(0.75, within(1e-12));
        assertThat(gate.selfPreserving()).isFalse();
    }

    /** 畸形入参 fail-fast：期望 0、阈值越界、负续约数。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> new SelfPreservationGate(0, 0.15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedRenewals 不能小于 1");
        assertThatThrownBy(() -> new SelfPreservationGate(100, 1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threshold 须在 (0,1)");
        SelfPreservationGate gate = new SelfPreservationGate(100, 0.15);
        assertThatThrownBy(() -> gate.resetWindow(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("observedRenewals 不能为负");
    }
}
