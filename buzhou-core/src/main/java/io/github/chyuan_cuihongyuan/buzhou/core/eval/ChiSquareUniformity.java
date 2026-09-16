package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.Arrays;

/**
 * 卡方均匀性检验（spec 2048 / T3197 / impl 1599）——统计学经典（Pearson
 * χ² 拟合检验）思想：样本分桶频数 vs 均匀期望的偏离度量化——χ² =
 * Σ(O−E)²/E。评估输出分布（模型回答是否偏斜某些桶）/哈希分桶均匀性
 *（一致性哈希环负载）/采样公平性有了可判定口径：χ² 超临界值（自由度
 * k−1 的 0.05 显著水平表，内置常用 1–20 桶）即拒绝均匀假设。
 *
 * <p>纯函数零状态、确定性。
 */
public final class ChiSquareUniformity {

    /** 默认显著水平 0.05 的上侧临界值表（自由度 1..20——k 桶的自由度 k−1）。 */
    private static final double[] CRITICAL_005 = {
            3.841d, 5.991d, 7.815d, 9.488d, 11.070d, 12.592d, 14.067d, 15.507d,
            16.919d, 18.307d, 19.675d, 21.026d, 22.362d, 23.685d, 24.996d,
            26.296d, 27.587d, 28.869d, 30.144d, 31.410d
    };

    /** 检验结果：统计量 + 自由度 + 临界值 + 是否拒绝均匀。 */
    public record ChiSquareResult(double statistic, int degreesOfFreedom,
                                  double criticalValue, boolean rejectsUniform) {
    }

    private ChiSquareUniformity() {
    }

    /**
     * 检验观测频数是否偏离均匀分布：桶数 k ∈ [2,21]、每桶观测 ≥0、总
     * 观测 &gt; 0（期望 = 总/k）。χ² 超显著水平 0.05 临界值即拒绝。
     */
    public static ChiSquareResult test(long[] observedCounts) {
        if (observedCounts == null) {
            throw new IllegalArgumentException("observedCounts 不能为 null");
        }
        int k = observedCounts.length;
        if (k < 2 || k > 21) {
            throw new IllegalArgumentException("桶数须在 [2,21]：" + k);
        }
        long total = Arrays.stream(observedCounts).sum();
        if (total <= 0) {
            throw new IllegalArgumentException("总观测须 > 0");
        }
        double expected = (double) total / k;
        double statistic = 0.0d;
        for (long observed : observedCounts) {
            if (observed < 0) {
                throw new IllegalArgumentException("观测频数须 ≥ 0：" + observed);
            }
            double diff = observed - expected;
            statistic += diff * diff / expected;
        }
        int dof = k - 1;
        double critical = CRITICAL_005[dof - 1];
        return new ChiSquareResult(statistic, dof, critical, statistic > critical);
    }
}
