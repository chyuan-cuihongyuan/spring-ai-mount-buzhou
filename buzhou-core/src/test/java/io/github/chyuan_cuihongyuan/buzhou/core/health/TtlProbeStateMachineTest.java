package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1813 / T2828：TTL 探针三态——边界含上、新鲜度钳零、普查。 */
class TtlProbeStateMachineTest {

    /** 三态判定 + 边界（预警线含上、到期线含上）。 */
    @Test
    void shouldEvaluateThreeStatesWithInclusiveBoundaries() {
        long ttl = 1000L;
        double warn = 0.5d;
        assertThat(TtlProbeStateMachine.evaluate(400L, ttl, warn))
                .isEqualTo(TtlProbeStateMachine.ProbeState.PASSING);
        assertThat(TtlProbeStateMachine.evaluate(500L, ttl, warn))
                .isEqualTo(TtlProbeStateMachine.ProbeState.STALE);
        assertThat(TtlProbeStateMachine.evaluate(999L, ttl, warn))
                .isEqualTo(TtlProbeStateMachine.ProbeState.STALE);
        assertThat(TtlProbeStateMachine.evaluate(1000L, ttl, warn))
                .isEqualTo(TtlProbeStateMachine.ProbeState.CRITICAL);
        assertThat(TtlProbeStateMachine.evaluate(2000L, ttl, warn))
                .isEqualTo(TtlProbeStateMachine.ProbeState.CRITICAL);
    }

    /** 新鲜度读数：线性递减、到期后钳 0（负值不外泄）。 */
    @Test
    void freshnessClampsAtZeroAfterExpiry() {
        assertThat(TtlProbeStateMachine.freshness(0, 1000L)).isEqualTo(1.0d);
        assertThat(TtlProbeStateMachine.freshness(250, 1000L)).isEqualTo(0.75d);
        assertThat(TtlProbeStateMachine.freshness(1000, 1000L)).isZero();
        assertThat(TtlProbeStateMachine.freshness(5000, 1000L)).isZero();
    }

    /** 普查三态计数 + 到期占比；null 按空表哨兵。 */
    @Test
    void censusCountsAndSentinel() {
        TtlProbeStateMachine.Census census = TtlProbeStateMachine.census(1000L, 0.5d, List.of(
                new TtlProbeStateMachine.ProbeSample("p1", 100L),
                new TtlProbeStateMachine.ProbeSample("p2", 600L),
                new TtlProbeStateMachine.ProbeSample("p3", 1500L),
                new TtlProbeStateMachine.ProbeSample("p4", 1500L)));
        assertThat(census.probes()).isEqualTo(4);
        assertThat(census.passing()).isEqualTo(1);
        assertThat(census.stale()).isEqualTo(1);
        assertThat(census.critical()).isEqualTo(2);
        assertThat(census.criticalRatio()).isEqualTo(0.5d);
        assertThat(TtlProbeStateMachine.census(1000L, 0.5d, null).criticalRatio())
                .isEqualTo(-1d);
    }

    /** 畸形入参 fail-fast：负年龄、TTL < 1、warnFraction 越界/NaN、样本空 id。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> TtlProbeStateMachine.evaluate(-1, 1000L, 0.5d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TtlProbeStateMachine.evaluate(0, 0, 0.5d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttlMillis 不能小于 1");
        assertThatThrownBy(() -> TtlProbeStateMachine.evaluate(0, 1000L, 1.5d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("warnFraction 须在 [0,1]");
        assertThatThrownBy(() -> TtlProbeStateMachine.evaluate(0, 1000L, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TtlProbeStateMachine.ProbeSample("", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
