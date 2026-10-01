package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 矩阵链乘法（spec 11021 / Y11043 / impl 2474）——CLRS 15.2 思想（区间 DP
 * 同源）：**m[i][j]=min over k of m[i][k]+m[k+1][j]+p_{i−1}·p_k·p_j**——
 * 结合律括号化的最少标量乘法次数（区间 DP 面；Karger 随机化测试面勘误
 * 换静脉——确定性圣像口径）。
 *
 * <p>维度数组长度 ≥3（至少一矩阵）；维度非正 fail-fast；long 域；复算确定。
 */
public final class MatrixChainOrder {

    private MatrixChainOrder() {
    }

    /**
     * 最少标量乘法次数。
     *
     * @param dimensions 维度数组（长度 n+1 表 n 个矩阵；每维 ≥1）
     * @throws IllegalArgumentException null/长度不足/非正维度
     */
    public static long minMultiplications(int[] dimensions) {
        if (dimensions == null || dimensions.length < 3) {
            throw new IllegalArgumentException("维度数组长度 ≥3（实际 "
                    + (dimensions == null ? "null" : dimensions.length) + "）");
        }
        for (int i = 0; i < dimensions.length; i++) {
            if (dimensions[i] < 1) {
                throw new IllegalArgumentException("维度为正（第 " + i + " 位实际 "
                        + dimensions[i] + "）");
            }
        }
        int n = dimensions.length - 1;
        long[][] cost = new long[n + 1][n + 1];
        for (int length = 2; length <= n; length++) {
            for (int i = 1; i + length - 1 <= n; i++) {
                int j = i + length - 1;
                cost[i][j] = Long.MAX_VALUE;
                for (int k = i; k < j; k++) {
                    long candidate = cost[i][k] + cost[k + 1][j]
                            + (long) dimensions[i - 1] * dimensions[k] * dimensions[j];
                    cost[i][j] = Math.min(cost[i][j], candidate);
                }
            }
        }
        return cost[1][n];
    }
}
