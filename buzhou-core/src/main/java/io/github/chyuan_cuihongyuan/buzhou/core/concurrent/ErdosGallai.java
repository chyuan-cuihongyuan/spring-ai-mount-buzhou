package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Arrays;

/**
 * 图序列可图化判定（spec 11020 / Y11041 / impl 2473）——Erdős–Gallai 1960
 * 思想（NetworkX is_graphical 同源）：**降序度序列满足 Σd 偶 + 逐前缀
 * Σ_{i≤k}d_i ≤ k(k−1)+Σ_{i>k}min(d_i,k)**⟺ 简单图存在——不等式族判定面。
 *
 * <p>负度 IllegalArgumentException；奇和非为布尔假（合法无解非异常）；
 * null fail-fast；复算确定。
 */
public final class ErdosGallai {

    private ErdosGallai() {
    }

    /**
     * 度序列是否可由简单图实现。
     *
     * @param degrees 度序列（非负）
     * @return 可图化为真
     * @throws IllegalArgumentException null/负度
     */
    public static boolean isGraphical(int[] degrees) {
        if (degrees == null) {
            throw new IllegalArgumentException("度序列非 null");
        }
        int sum = 0;
        for (int i = 0; i < degrees.length; i++) {
            if (degrees[i] < 0) {
                throw new IllegalArgumentException("度非负（第 " + i + " 位实际 "
                        + degrees[i] + "）");
            }
            sum += degrees[i];
        }
        if (sum % 2 != 0) {
            return false;
        }
        int[] sorted = degrees.clone();
        Arrays.sort(sorted);
        for (int k = 1; k <= sorted.length; k++) {
            int prefix = 0;
            for (int i = 0; i < k; i++) {
                prefix += sorted[sorted.length - 1 - i];
            }
            int suffixSum = 0;
            for (int i = k; i < sorted.length; i++) {
                suffixSum += Math.min(sorted[sorted.length - 1 - i], k);
            }
            if (prefix > k * (k - 1) + suffixSum) {
                return false;
            }
        }
        return true;
    }
}
