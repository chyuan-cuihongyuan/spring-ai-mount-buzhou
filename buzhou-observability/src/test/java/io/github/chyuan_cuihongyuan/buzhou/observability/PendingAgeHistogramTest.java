package io.github.chyuan_cuihongyuan.buzhou.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1733 / T2668：PendingAgeHistogram 直测——lag 分桶/哨戒/负值忽略。
 */
class PendingAgeHistogramTest {

    @Test
    void defaultBucketsAssignByAge() {
        var histogram = new PendingAgeHistogram();
        histogram.record(500L);
        histogram.record(5_000L);
        histogram.record(30_000L);
        histogram.record(120_000L);
        histogram.record(400_000L);
        long[] counts = histogram.bucketCounts();
        assertThat(counts).hasSize(5);
        for (long count : counts) {
            assertThat(count).isEqualTo(1);
        }
        assertThat(histogram.total()).isEqualTo(5);
        assertThat(histogram.oldestMillis()).isEqualTo(400_000L);
    }

    @Test
    void negativeIgnored() {
        var histogram = new PendingAgeHistogram();
        histogram.record(-1L);
        assertThat(histogram.total()).isZero();
        assertThat(histogram.oldestMillis()).isZero();
    }

    @Test
    void customBounds() {
        var histogram = new PendingAgeHistogram(new long[]{100L});
        histogram.record(50L);
        histogram.record(150L);
        assertThat(histogram.bucketCounts()).hasSize(2);
        assertThat(histogram.bucketCounts()[0]).isEqualTo(1);
        assertThat(histogram.bucketCounts()[1]).isEqualTo(1);
    }
}
