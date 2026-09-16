package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.Arrays;

/**
 * Benjamini-Hochberg FDR 校正（spec 2060 / T3221 / impl 1611）——
 * BH 程序思想：控制**假发现率**（FDR——显著集合中假阳性的期望比例）
 * 而非族错误率（FWER——任一假阳性概率）：探索性评估（找候选指标/找
 * 有效应子集）宁多勿漏，FWER 的 Holm 过紧——BH 阈值 j/m×α 随序号
 * 放宽，功效显著更高，代价是显著集合期望含 ≤α 比例假阳性。
 *
 * <p>纯函数零状态、确定性；与 Holm 成对：确证性实验用 Holm（FWER），
 * 探索性筛选用 BH（FDR）。
 */
public final class FalseDiscoveryRate {

    /** 默认 FDR 水平（0.05——显著集合期望假阳性占比 ≤5%）。 */
    public static final double DEFAULT_Q = 0.05d;

    private FalseDiscoveryRate() {
    }

    /**
     * BH 程序：p 升序找最大 j 使 p₍ⱼ₎ ≤ (j/m)·q——前 j 个（原索引）
     * 全显著、其余不显著。契约：p ∈ [0,1] 非 NaN、q ∈ (0,1)、非空
     * fail-fast。
     */
    public static boolean[] benjaminiHochberg(double[] pValues, double q) {
        if (pValues == null || pValues.length == 0) {
            throw new IllegalArgumentException("pValues 不能为空");
        }
        if (!(q > 0) || q >= 1 || Double.isNaN(q)) {
            throw new IllegalArgumentException("q 须在 (0,1)：" + q);
        }
        for (double p : pValues) {
            if (!(p >= 0) || p > 1 || Double.isNaN(p)) {
                throw new IllegalArgumentException("p 须在 [0,1]：" + p);
            }
        }
        int m = pValues.length;
        Integer[] order = new Integer[m];
        for (int i = 0; i < m; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(pValues[a], pValues[b]));
        // 找最大 j 满足 p(j) ≤ (j/m)q
        int cutoff = 0;
        for (int j = 1; j <= m; j++) {
            if (pValues[order[j - 1]] <= (double) j / m * q) {
                cutoff = j;
            }
        }
        boolean[] significant = new boolean[m];
        for (int j = 0; j < cutoff; j++) {
            significant[order[j]] = true;
        }
        return significant;
    }
}
