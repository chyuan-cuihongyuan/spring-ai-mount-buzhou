package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1860 / T2922：对数桶——桶号、分位数误差界、正值域契约。 */
class LogBucketHistogramTest {

    /** 桶号：γ=2 时 1→0、2→1、3.9→1、4→2。 */
    @Test
    void shouldComputeBucketIndices() {
        assertThat(LogBucketHistogram.bucketIndex(1d, 2d)).isZero();
        assertThat(LogBucketHistogram.bucketIndex(2d, 2d)).isEqualTo(1);
        assertThat(LogBucketHistogram.bucketIndex(3.9d, 2d)).isEqualTo(1);
        assertThat(LogBucketHistogram.bucketIndex(4d, 2d)).isEqualTo(2);
        assertThat(LogBucketHistogram.bucketIndex(0.5d, 2d)).isEqualTo(-1);
    }

    /** 分位数误差界：1000 个 [1,1000) 均匀样本的 P95 与精确值差 ≤ γ−1。 */
    @Test
    void approxQuantileWithinRelativeErrorBound() {
        List<Double> samples = IntStream.range(1, 1001)
                .mapToObj(i -> (double) i).toList();
        LogBucketHistogram.ApproxQuantile approx =
                LogBucketHistogram.quantile(samples, 0.95, 1.25d);
        double exact = samples.get(samples.size() - 50); // P95 精确（最近秩）
        double relativeDiff = Math.abs(approx.estimate() - exact) / exact;
        assertThat(approx.relativeErrorBound()).isEqualTo(0.25d);
        // 中点估计 + 桶界：实际误差应远小于界（界是保守上界）
        assertThat(relativeDiff).isLessThan(approx.relativeErrorBound());
    }

    /** 中位数与最大值可用（q=0.5/q=1 两端）。 */
    @Test
    void medianAndMaxQuantilesAvailable() {
        List<Double> samples = List.of(10d, 100d, 1000d);
        double median = LogBucketHistogram.quantile(samples, 0.5, 2d).estimate();
        assertThat(median).isGreaterThan(0);
        double max = LogBucketHistogram.quantile(samples, 1.0, 2d).estimate();
        assertThat(max).isGreaterThanOrEqualTo(99d); // 1000 落桶 [512,1024)
    }

    /** 正值域与畸形 fail-fast：0/负值、γ≤1、q 越界、空样本。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> LogBucketHistogram.bucketIndex(0d, 2d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value 须为正数");
        assertThatThrownBy(() -> LogBucketHistogram.bucketIndex(-1d, 2d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LogBucketHistogram.bucketIndex(1d, 1d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gamma 须 > 1");
        assertThatThrownBy(() -> LogBucketHistogram.quantile(List.of(1d), 0d, 2d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("q 须在 (0,1]");
        assertThatThrownBy(() -> LogBucketHistogram.quantile(List.of(), 0.5, 2d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("样本不能为空");
    }
}
