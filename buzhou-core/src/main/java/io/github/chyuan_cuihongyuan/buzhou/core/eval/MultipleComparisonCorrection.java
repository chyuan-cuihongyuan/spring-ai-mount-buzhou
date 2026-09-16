package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.Arrays;

/**
 * 多重比较校正（spec 2058 / T3217 / impl 1609）——Bonferroni / Holm
 * 思想：一次评估同时看 m 个指标的显著性，α=0.05 下期望 ~m/20 个假
 * 阳性（「多测必显灵」）——校正两法：Bonferroni（p×m——最保守、单
 * 步）与 Holm-Bonferroni（p 值排序逐步放大比较阈值——族错误率同控
 * 而功效更高，即 Holm 严格不弱于 Bonferroni）。
 *
 * <p>纯函数零状态、确定性。
 */
public final class MultipleComparisonCorrection {

    /** 默认族显著水平（0.05）。 */
    public static final double DEFAULT_ALPHA = 0.05d;

    /** 校正判定结果：各 p 值是否在校正后显著。 */
    public record Verdict(boolean[] significant, int significantCount, String method) {
    }

    private MultipleComparisonCorrection() {
    }

    /** Bonferroni：pᵢ × m ≤ α 即显著（最保守单步）。契约：p ∈ [0,1]、alpha ∈ (0,1)。 */
    public static Verdict bonferroni(double[] pValues, double alpha) {
        validate(pValues, alpha);
        boolean[] significant = new boolean[pValues.length];
        int count = 0;
        for (int i = 0; i < pValues.length; i++) {
            significant[i] = pValues[i] * pValues.length <= alpha;
            if (significant[i]) {
                count++;
            }
        }
        return new Verdict(significant, count, "bonferroni");
    }

    /**
     * Holm-Bonferroni：p 升序，第 j 小者（j 从 1 起）与 α/(m−j+1) 比
     * ——**首次不显著即止**（其后全部不显著——单调停步），族错误率
     * 同控 FWER 而功效恒不弱于 Bonferroni。
     */
    public static Verdict holm(double[] pValues, double alpha) {
        validate(pValues, alpha);
        int m = pValues.length;
        Integer[] order = new Integer[m];
        for (int i = 0; i < m; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(pValues[a], pValues[b]));
        boolean[] significant = new boolean[m];
        int count = 0;
        boolean stopped = false;
        for (int j = 1; j <= m; j++) {
            int idx = order[j - 1];
            double threshold = alpha / (m - j + 1);
            if (!stopped && pValues[idx] <= threshold) {
                significant[idx] = true;
                count++;
            } else {
                stopped = true; // 首次不显著即止——其后全不显著
            }
        }
        return new Verdict(significant, count, "holm");
    }

    private static void validate(double[] pValues, double alpha) {
        if (pValues == null || pValues.length == 0) {
            throw new IllegalArgumentException("pValues 不能为空");
        }
        if (!(alpha > 0) || alpha >= 1 || Double.isNaN(alpha)) {
            throw new IllegalArgumentException("alpha 须在 (0,1)：" + alpha);
        }
        for (double p : pValues) {
            if (!(p >= 0) || p > 1 || Double.isNaN(p)) {
                throw new IllegalArgumentException("p 须在 [0,1]：" + p);
            }
        }
    }
}
