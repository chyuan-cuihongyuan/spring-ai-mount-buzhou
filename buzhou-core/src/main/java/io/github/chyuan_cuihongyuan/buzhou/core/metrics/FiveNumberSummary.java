package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * 五数概括与 IQR 围栏（spec 2056 / T3213 / impl 1607）——箱线图统计
 *（Tukey 探索性数据分析）思想：min / Q1 / 中位 / Q3 / max 五点概括
 * 分布 + IQR 围栏（Q1−1.5×IQR，Q3+1.5×IQR）——围栏外即**离群点**
 *（离群判定有了经典口径，不再拍 σ 阈值）；分位插值用线性口径
 *（R-7，与直方图分位同族惯例）。
 *
 * <p>纯函数零状态、确定性。
 */
public final class FiveNumberSummary {

    /** 默认围栏系数（Tukey 1.5×IQR 惯例——内围栏）。 */
    public static final double DEFAULT_FENCE_FACTOR = 1.5d;

    /** 五数 + 围栏读数。 */
    public record Summary(double min, double q1, double median, double q3, double max,
                          double lowerFence, double upperFence) {

        /** 值是否离群（围栏外）。 */
        public boolean isOutlier(double value) {
            return value < lowerFence || value > upperFence;
        }

        /** IQR = Q3 − Q1。 */
        public double iqr() {
            return q3 - q1;
        }
    }

    private FiveNumberSummary() {
    }

    /**
     * 概括：样本排序后五点 + 1.5×IQR 围栏。分位插值线性（R-7：p 位
     * 置 h=(n−1)p，floor 与 ceil 线性）。契约：样本非空非 NaN。
     */
    public static Summary of(double[] samples) {
        return of(samples, DEFAULT_FENCE_FACTOR);
    }

    /** 自定义围栏系数版（系数 ≥ 0）。 */
    public static Summary of(double[] samples, double fenceFactor) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("样本不能为空");
        }
        if (!(fenceFactor >= 0) || Double.isNaN(fenceFactor)) {
            throw new IllegalArgumentException("fenceFactor 须 ≥ 0：" + fenceFactor);
        }
        double[] sorted = samples.clone();
        Arrays.sort(sorted);
        for (double v : sorted) {
            if (Double.isNaN(v)) {
                throw new IllegalArgumentException("样本不能含 NaN");
            }
        }
        double min = sorted[0];
        double max = sorted[sorted.length - 1];
        double q1 = quantile(sorted, 0.25d);
        double median = quantile(sorted, 0.50d);
        double q3 = quantile(sorted, 0.75d);
        double iqr = q3 - q1;
        return new Summary(min, q1, median, q3, max,
                q1 - fenceFactor * iqr, q3 + fenceFactor * iqr);
    }

    /** R-7 线性分位：h=(n−1)p，位置 h 的 floor/ceil 线性插值。 */
    private static double quantile(double[] sorted, double p) {
        if (sorted.length == 1) {
            return sorted[0];
        }
        double h = (sorted.length - 1) * p;
        int lo = (int) Math.floor(h);
        int hi = Math.min(lo + 1, sorted.length - 1);
        double frac = h - lo;
        return sorted[lo] * (1 - frac) + sorted[hi] * frac;
    }
}
