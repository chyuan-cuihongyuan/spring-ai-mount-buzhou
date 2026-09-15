package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1711 / T2624：TurnInterArrivalStats 纯函数直测——间隔账目/中位/p95/哨兵。
 */
class TurnInterArrivalStatsTest {

    @Test
    void intervalsMedianAndP95() {
        var report = TurnInterArrivalStats.analyze(List.of(0L, 1000L, 2100L, 3000L, 4000L, 10000L));
        assertThat(report.turns()).isEqualTo(6);
        assertThat(report.intervalsMillis()).containsExactly(1000L, 1100L, 900L, 1000L, 6000L);
        assertThat(report.medianMillis()).isEqualTo(1000L);
        assertThat(report.p95Millis()).isEqualTo(6000L);
    }

    @Test
    void evenCountMedianIsMidAverage() {
        var report = TurnInterArrivalStats.analyze(List.of(0L, 100L, 301L));
        assertThat(report.intervalsMillis()).containsExactly(100L, 201L);
        assertThat(report.medianMillis()).isEqualTo(150L);
        assertThat(report.p95Millis()).isEqualTo(201L);
    }

    @Test
    void degenerateSizesCarrySentinel() {
        var single = TurnInterArrivalStats.analyze(List.of(7L));
        assertThat(single.turns()).isEqualTo(1);
        assertThat(single.medianMillis()).isEqualTo(-1L);
        assertThat(single.p95Millis()).isEqualTo(-1L);
        assertThat(single.intervalsMillis()).isEmpty();
        assertThat(TurnInterArrivalStats.analyze(null).turns()).isZero();
    }
}
