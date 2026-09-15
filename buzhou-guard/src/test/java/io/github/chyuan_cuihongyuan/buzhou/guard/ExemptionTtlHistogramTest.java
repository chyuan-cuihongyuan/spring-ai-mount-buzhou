package io.github.chyuan_cuihongyuan.buzhou.guard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1739 / T2680：ExemptionTtlHistogram 直测——分桶/永久哨戒。
 */
class ExemptionTtlHistogramTest {

    @Test
    void defaultBucketsAndPermanent() {
        var histogram = new ExemptionTtlHistogram();
        histogram.record(30_000L);
        histogram.record(120_000L);
        histogram.record(1_200_000L);
        histogram.record(7_200_000L);
        histogram.record(172_800_000L);
        histogram.record(0);
        histogram.record(-5);
        long[] counts = histogram.bucketCounts();
        assertThat(counts).hasSize(5);
        assertThat(counts[0]).isEqualTo(1);
        assertThat(counts[1]).isEqualTo(1);
        assertThat(counts[2]).isEqualTo(1);
        assertThat(counts[3]).isEqualTo(1);
        assertThat(counts[4]).isEqualTo(1);
        assertThat(histogram.permanentCount()).isEqualTo(1);
        assertThat(histogram.total()).isEqualTo(6);
    }

    @Test
    void customBounds() {
        var histogram = new ExemptionTtlHistogram(new long[]{1000L});
        histogram.record(500L);
        histogram.record(2000L);
        assertThat(histogram.bucketCounts()).hasSize(2);
        assertThat(histogram.bucketCounts()[0]).isEqualTo(1);
        assertThat(histogram.bucketCounts()[1]).isEqualTo(1);
    }
}
