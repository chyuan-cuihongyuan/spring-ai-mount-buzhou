package io.github.chyuan_cuihongyuan.buzhou.core.eval;

/**
 * Wilson score 置信区间（spec 1630 / T2411，统计报告标准工具——比正态近似
 * 在小样本/极端比例下不越界不出负值）：胜率点估计之外给出不确定性显形
 * ——「0.7 胜率（95% CI [0.42, 0.88]）」与「0.7 胜率」是两个结论强度。
 *
 * <p>与 SPRT（spec 1605 序贯判定）互补：那是「现在能不能停」的决策面，
 * 这是「点估计有多可信」的报告面。无连续性校正（Wald-Wilson 原式）。
 * @since 1.0.0
 */
public final class WilsonInterval {

    /** 默认 z 值（95% 置信）。 */
    public static final double Z_95 = 1.96;

    private WilsonInterval() {
    }

    /**
     * Wilson score 区间：成功数 successes / 样本 n（n ≤ 0 或 successes 越界 → [0,0]）。
     * 返回 [low, high]（均 ∈ [0,1]——数学上不越界，夹取是浮点兜底）。
     */
    public static double[] of(int successes, int n) {
        return of(successes, n, Z_95);
    }

    /** 带置信水平的区间（z 须为正——常规 1.645/1.96/2.576）。 */
    public static double[] of(int successes, int n, double z) {
        if (n <= 0 || successes < 0 || successes > n || !(z > 0)) {
            return new double[]{0.0, 0.0};
        }
        double p = (double) successes / n;
        double z2 = z * z;
        double denom = 1 + z2 / n;
        double center = (p + z2 / (2 * n)) / denom;
        double half = z * Math.sqrt(p * (1 - p) / n + z2 / (4.0 * n * n)) / denom;
        return new double[]{Math.max(0.0, center - half), Math.min(1.0, center + half)};
    }
}
