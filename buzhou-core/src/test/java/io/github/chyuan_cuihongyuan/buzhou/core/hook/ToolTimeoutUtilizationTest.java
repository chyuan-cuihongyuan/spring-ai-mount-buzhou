package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1716 / T2634：ToolTimeoutUtilization 直测——利用率分桶/上限/非法限值。
 */
class ToolTimeoutUtilizationTest {

    @Test
    void defaultBucketsSpreadByRatio() {
        var histogram = new ToolTimeoutUtilization();
        histogram.record(100, 1000);
        histogram.record(300, 1000);
        histogram.record(600, 1000);
        histogram.record(800, 1000);
        histogram.record(950, 1000);
        histogram.record(1500, 1000);
        long[] counts = histogram.bucketCounts();
        assertThat(counts).hasSize(6);
        assertThat(counts[0]).isEqualTo(1);
        assertThat(counts[1]).isEqualTo(1);
        assertThat(counts[2]).isEqualTo(1);
        assertThat(counts[3]).isEqualTo(1);
        assertThat(counts[4]).isEqualTo(1);
        assertThat(counts[5]).isEqualTo(1);
        assertThat(histogram.maxRatio()).isEqualTo(1.5d);
        assertThat(histogram.total()).isEqualTo(6);
    }

    @Test
    void invalidLimitIgnored() {
        var histogram = new ToolTimeoutUtilization();
        histogram.record(100, 0);
        histogram.record(100, -5);
        assertThat(histogram.total()).isZero();
        assertThat(histogram.maxRatio()).isZero();
    }

    @Test
    void singleBucketWithoutBounds() {
        var histogram = new ToolTimeoutUtilization(new double[0]);
        histogram.record(10, 100);
        assertThat(histogram.bucketCounts()).hasSize(1);
        assertThat(histogram.bucketCounts()[0]).isEqualTo(1);
    }
}
