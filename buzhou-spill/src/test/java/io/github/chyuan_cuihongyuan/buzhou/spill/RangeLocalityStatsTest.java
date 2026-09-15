package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1742 / T2686：RangeLocalityStats 直测——连续判定/首读/哨兵。
 */
class RangeLocalityStatsTest {

    @Test
    void sequentialVsRandomClassification() {
        var stats = new RangeLocalityStats();
        stats.record(0, 100);
        stats.record(100, 100);
        stats.record(500, 100);
        stats.record(200, 50);
        var census = stats.census();
        assertThat(census.reads()).isEqualTo(4);
        assertThat(census.sequential()).isEqualTo(1);
        assertThat(census.random()).isEqualTo(2);
        assertThat(census.sequentialShare()).isCloseTo(1d / 3d, within(1e-9));
    }

    @Test
    void firstReadIsUnclassified() {
        var stats = new RangeLocalityStats();
        stats.record(0, 10);
        var census = stats.census();
        assertThat(census.sequentialShare()).isEqualTo(-1d);
        assertThat(census.sequential()).isZero();
        assertThat(census.random()).isZero();
    }

    @Test
    void negativeIgnoredAndEmpty() {
        var stats = new RangeLocalityStats();
        assertThat(stats.census().reads()).isZero();
        stats.record(-1, 10);
        assertThat(stats.census().reads()).isZero();
    }
}
