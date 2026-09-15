package io.github.chyuan_cuihongyuan.buzhou.core.fact;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1724 / T2650：FactReadHistogram 直测——四档分桶/基数并桶/守恒。
 */
class FactReadHistogramTest {

    @Test
    void heatBandsAssignByReadCount() {
        var histogram = new FactReadHistogram();
        histogram.record("once");
        histogram.record("warm");
        histogram.record("warm");
        histogram.record("warm");
        for (int i = 0; i < 5; i++) {
            histogram.record("hot");
        }
        for (int i = 0; i < 20; i++) {
            histogram.record("blazing");
        }
        var census = histogram.census();
        assertThat(census.totalReads()).isEqualTo(29);
        assertThat(census.distinctKeys()).isEqualTo(4);
        assertThat(census.cold()).isEqualTo(1);
        assertThat(census.warm()).isEqualTo(1);
        assertThat(census.hot()).isEqualTo(1);
        assertThat(census.blazing()).isEqualTo(1);
    }

    @Test
    void overflowBucketsBeyondCapacity() {
        var histogram = new FactReadHistogram(2);
        histogram.record("a");
        histogram.record("b");
        histogram.record("c");
        assertThat(histogram.census().distinctKeys()).isEqualTo(3);
        assertThat(histogram.census().cold()).isEqualTo(3);
    }

    @Test
    void blankKeysGoAnonymous() {
        var histogram = new FactReadHistogram();
        histogram.record(null);
        histogram.record(" ");
        var census = histogram.census();
        assertThat(census.distinctKeys()).isEqualTo(1);
        assertThat(census.totalReads()).isEqualTo(2);
    }
}
