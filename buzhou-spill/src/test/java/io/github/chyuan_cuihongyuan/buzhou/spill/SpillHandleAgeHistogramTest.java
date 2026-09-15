package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1743 / T2688：SpillHandleAgeHistogram 直测——分桶/哨戒/负值忽略。
 */
class SpillHandleAgeHistogramTest {

    @Test
    void defaultBucketsAssignByAge() {
        var histogram = new SpillHandleAgeHistogram();
        histogram.record(30_000L);
        histogram.record(600_000L);
        histogram.record(50_000_000L);
        histogram.record(200_000_000L);
        long[] counts = histogram.bucketCounts();
        assertThat(counts).hasSize(4);
        assertThat(counts[0]).isEqualTo(1);
        assertThat(counts[1]).isEqualTo(1);
        assertThat(counts[2]).isEqualTo(1);
        assertThat(counts[3]).isEqualTo(1);
        assertThat(histogram.eldestMillis()).isEqualTo(200_000_000L);
        assertThat(histogram.total()).isEqualTo(4);
    }

    @Test
    void negativeIgnored() {
        var histogram = new SpillHandleAgeHistogram();
        histogram.record(-1L);
        assertThat(histogram.total()).isZero();
        assertThat(histogram.eldestMillis()).isZero();
    }
}
