package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1707 / T2616：SessionAgeHistogram 直测——分桶/守恒/哨戒/负值忽略。
 */
class SessionAgeHistogramTest {

    @Test
    void defaultBucketsAssignExclusiveBands() {
        var histogram = new SessionAgeHistogram();
        histogram.record(500L);
        histogram.record(3_600_000L);
        histogram.record(86_400_000L);
        histogram.record(604_800_001L);
        long[] counts = histogram.bucketCounts();
        assertThat(counts).hasSize(4);
        assertThat(counts[0]).isEqualTo(1);
        assertThat(counts[1]).isEqualTo(1);
        assertThat(counts[2]).isEqualTo(1);
        assertThat(counts[3]).isEqualTo(1);
        assertThat(histogram.total()).isEqualTo(4);
        assertThat(histogram.eldestMillis()).isEqualTo(604_800_001L);
    }

    @Test
    void negativeValuesIgnored() {
        var histogram = new SessionAgeHistogram();
        histogram.record(-5L);
        assertThat(histogram.total()).isZero();
        assertThat(histogram.eldestMillis()).isZero();
    }

    @Test
    void customBoundsProduceNBucketsPlusOne() {
        var histogram = new SessionAgeHistogram(new long[]{100L});
        histogram.record(50L);
        histogram.record(500L);
        assertThat(histogram.bucketCounts()).hasSize(2);
        assertThat(histogram.bucketCounts()[0]).isEqualTo(1);
        assertThat(histogram.bucketCounts()[1]).isEqualTo(1);
    }
}
