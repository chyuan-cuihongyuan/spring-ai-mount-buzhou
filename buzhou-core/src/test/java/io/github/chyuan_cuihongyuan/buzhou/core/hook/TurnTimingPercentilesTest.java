package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TurnTimingPercentilesTest {

    private static HookEnvironment env() {
        return new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
    }

    @Test
    void r7InterpolationKnownValues() {
        List<Long> samples = List.of(10L, 20L, 30L, 40L);

        assertThat(TurnTimingHook.percentile(samples, 0.50)).isEqualTo(25.0);
        assertThat(TurnTimingHook.percentile(samples, 0.95)).isEqualTo(38.5);
        assertThat(TurnTimingHook.percentile(samples, 0.0)).isEqualTo(10.0);
        assertThat(TurnTimingHook.percentile(samples, 1.0)).isEqualTo(40.0);
    }

    @Test
    void singleSampleIsItsOwnQuantile() {
        assertThat(TurnTimingHook.percentile(List.of(7L), 0.0)).isEqualTo(7.0);
        assertThat(TurnTimingHook.percentile(List.of(7L), 0.95)).isEqualTo(7.0);
        assertThat(TurnTimingHook.percentile(List.of(7L), 1.0)).isEqualTo(7.0);
    }

    @Test
    void outOfRangeQuantileIsRejected() {
        List<Long> samples = List.of(1L, 2L);
        assertThatThrownBy(() -> TurnTimingHook.percentile(samples, -0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TurnTimingHook.percentile(samples, 1.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TurnTimingHook.percentile(List.of(), 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownSessionReturnsZeroRow() {
        TurnTimingHook hook = new TurnTimingHook();

        TurnLatencyPercentiles percentiles = hook.percentiles("ghost");
        assertThat(percentiles.count()).isZero();
        assertThat(percentiles.p50Millis()).isZero();
        assertThat(percentiles.p95Millis()).isZero();
        assertThat(percentiles.maxMillis()).isZero();
    }

    @Test
    void oneRealTurnYieldsSingleSampleRow() {
        TurnTimingHook hook = new TurnTimingHook();
        hook.beforeTurn(new DefaultTurnContext(env(), "q"));
        hook.afterTurn(new DefaultTurnContext(env(), "q"));

        TurnLatencyPercentiles percentiles = hook.percentiles("s1");
        assertThat(percentiles.count()).isEqualTo(1);
        assertThat(percentiles.p50Millis()).isGreaterThanOrEqualTo(0);
        assertThat(percentiles.p95Millis()).isGreaterThanOrEqualTo(0);
        assertThat(percentiles.p50Millis()).isEqualTo(percentiles.p95Millis());
        assertThat(percentiles.maxMillis()).isGreaterThanOrEqualTo(0);
    }
}
