package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * Theil-Sen 稳健斜率（spec 4038 / T6077 / impl 2139）——成对
 * 斜率中位数估计思想（Theil–Sen；scipy theilslopes）：斜率 =
 * 全部成对斜率（竖直对跳过）的中位数——崩溃点 ≈29%（近三成
 * 点被污染仍稳）；截距 = median(y_i − slope·x_i)（Sen 口径）。
 * 最小二乘对离群点零抗性（单点杠杆拉偏整条线）的病解。
 *
 * <p>中位数偶数取<b>下中位</b>（确定性；SpeculativeStraggler
 * 同款约定）；O(n²) 小样本口径；纯函数无状态。
 */
public final class TheilSenSlope {

    private TheilSenSlope() {
    }

    /**
     * 拟合（xs/ys 不齐 / n&lt;2 / 竖直退化 / 非有限值 fail-fast）。
     *
     * @param xs 自变量（需含至少两个互异值）
     * @param ys 因变量
     * @return 斜率+截距拟合
     */
    public static Fit fit(double[] xs, double[] ys) {
        if (xs == null || ys == null || xs.length != ys.length || xs.length < 2) {
            throw new IllegalArgumentException("xs/ys 等长且 n≥2："
                    + (xs == null ? -1 : xs.length) + "/" + (ys == null ? -1 : ys.length));
        }
        for (int i = 0; i < xs.length; i++) {
            if (!Double.isFinite(xs[i]) || !Double.isFinite(ys[i])) {
                throw new IllegalArgumentException("样本需有限：[" + i + "]=" + xs[i] + "," + ys[i]);
            }
        }
        double[] slopes = new double[xs.length * (xs.length - 1) / 2];
        int count = 0;
        for (int i = 0; i < xs.length; i++) {
            for (int j = i + 1; j < xs.length; j++) {
                if (xs[j] == xs[i]) {
                    continue;   // 竖直对跳过
                }
                slopes[count++] = (ys[j] - ys[i]) / (xs[j] - xs[i]);
            }
        }
        if (count == 0) {
            throw new IllegalArgumentException("竖直退化（无可比对 x 全同）");
        }
        double slope = lowerMedian(Arrays.copyOf(slopes, count));
        double[] residuals = new double[xs.length];
        for (int i = 0; i < xs.length; i++) {
            residuals[i] = ys[i] - slope * xs[i];
        }
        return new Fit(slope, lowerMedian(residuals));
    }

    /** 下中位（偶数取左——确定性）。 */
    private static double lowerMedian(double[] values) {
        Arrays.sort(values);
        return values[(values.length - 1) / 2];
    }

    /**
     * 拟合结果（不可变）。
     *
     * @param slope 稳健斜率（成对斜率中位数）
     * @param intercept Sen 截距（残差中位数）
     */
    public record Fit(double slope, double intercept) {

        /** 拟合线在 x 处的预测读数。 */
        public double predict(double x) {
            return slope * x + intercept;
        }
    }
}
