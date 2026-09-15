package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1808 / T2818：驱逐阈值门——硬即逐/软宽限/宽限满即逐/三态普查。 */
class EvictionThresholdGateTest {

    /** 硬阈即逐（宽限再长也不豁免）；软阈内待观察；软阈宽限满即逐。 */
    @Test
    void shouldDecideThreeStates() {
        EvictionThresholdGate.Thresholds t =
                new EvictionThresholdGate.Thresholds(0.7d, 0.9d);
        assertThat(EvictionThresholdGate.decide(0.5d, t, 0, 1000))
                .isEqualTo(EvictionThresholdGate.Decision.BELOW);
        assertThat(EvictionThresholdGate.decide(0.75d, t, 500, 1000))
                .isEqualTo(EvictionThresholdGate.Decision.GRACE_PENDING);
        assertThat(EvictionThresholdGate.decide(0.75d, t, 1000, 1000))
                .isEqualTo(EvictionThresholdGate.Decision.EVICT_NOW);
        assertThat(EvictionThresholdGate.decide(0.95d, t, 0, 1000))
                .isEqualTo(EvictionThresholdGate.Decision.EVICT_NOW);
    }

    /** 普查三态计数 + 立即逐占比；null 按空表。 */
    @Test
    void censusCountsThreeStates() {
        EvictionThresholdGate.Thresholds t =
                new EvictionThresholdGate.Thresholds(0.7d, 0.9d);
        EvictionThresholdGate.DecisionCensus census = EvictionThresholdGate.census(t, 1000,
                List.of(new EvictionThresholdGate.SignalSample(0.5d, 0),
                        new EvictionThresholdGate.SignalSample(0.5d, 0),
                        new EvictionThresholdGate.SignalSample(0.75d, 10),
                        new EvictionThresholdGate.SignalSample(0.75d, 5000),
                        new EvictionThresholdGate.SignalSample(0.99d, 0)));
        assertThat(census.signals()).isEqualTo(5);
        assertThat(census.below()).isEqualTo(2);
        assertThat(census.pending()).isEqualTo(1);
        assertThat(census.evict()).isEqualTo(2);
        assertThat(census.evictRatio()).isEqualTo(0.4d);
        assertThat(EvictionThresholdGate.census(t, 1000, null).signals()).isZero();
    }

    /** 空普查哨兵：evictRatio -1。 */
    @Test
    void emptyCensusYieldsSentinel() {
        EvictionThresholdGate.DecisionCensus census = EvictionThresholdGate.census(
                new EvictionThresholdGate.Thresholds(0.7d, 0.9d), 1000, List.of());
        assertThat(census.evictRatio()).isEqualTo(-1d);
    }

    /** 畸形入参 fail-fast：阈值倒挂/NaN、负毫秒、NaN 信号。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> new EvictionThresholdGate.Thresholds(0.9d, 0.7d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 ≤ soft ≤ hard");
        assertThatThrownBy(() -> new EvictionThresholdGate.Thresholds(Double.NaN, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvictionThresholdGate.decide(Double.NaN,
                new EvictionThresholdGate.Thresholds(0.7d, 0.9d), 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvictionThresholdGate.decide(0.5d,
                new EvictionThresholdGate.Thresholds(0.7d, 0.9d), -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
