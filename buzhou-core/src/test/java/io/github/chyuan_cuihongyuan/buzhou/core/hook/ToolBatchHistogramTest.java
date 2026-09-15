package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1712 / T2626：ToolBatchHistogram 直测——批规模分桶/哨戒/负值忽略。
 */
class ToolBatchHistogramTest {

    @Test
    void defaultBucketsSeparateParallelism() {
        var histogram = new ToolBatchHistogram();
        histogram.record(1);
        histogram.record(2);
        histogram.record(3);
        histogram.record(4);
        histogram.record(7);
        long[] counts = histogram.bucketCounts();
        assertThat(counts).hasSize(5);
        assertThat(counts[0]).isEqualTo(1);
        assertThat(counts[1]).isEqualTo(1);
        assertThat(counts[2]).isEqualTo(1);
        assertThat(counts[3]).isEqualTo(1);
        assertThat(counts[4]).isEqualTo(1);
        assertThat(histogram.total()).isEqualTo(5);
        assertThat(histogram.largestBatch()).isEqualTo(7);
    }

    @Test
    void invalidSizesIgnored() {
        var histogram = new ToolBatchHistogram();
        histogram.record(0);
        histogram.record(-2);
        assertThat(histogram.total()).isZero();
        assertThat(histogram.largestBatch()).isZero();
    }

    @Test
    void customBounds() {
        var histogram = new ToolBatchHistogram(new long[]{3});
        histogram.record(1);
        histogram.record(5);
        assertThat(histogram.bucketCounts()).hasSize(2);
        assertThat(histogram.bucketCounts()[0]).isEqualTo(1);
        assertThat(histogram.bucketCounts()[1]).isEqualTo(1);
    }
}
