package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Yule–Walker 方程（spec 11012 / Y11025 / impl 2465）——Yule 1927/Walker 1931
 * 思想（Statsmodels 同源）：**含 r0 的自协方差域 AR 主方程**——r0 归一化后
 * 消费 DurbinLevinson（spec 11010）递推得系数，σ²=r0−Σa_j r_j 于未归一域
 * 复原（与 DurbinLevinson 同域不同面：自协方差域主方程 vs 归一化自相关域
 * 递推——库内互喂）。
 *
 * <p>r0&gt;0 必需（方差为零/负拒绝）；null/空 fail-fast；复算确定。
 */
public final class YuleWalker {

    private YuleWalker() {
    }

    /**
     * 自协方差序列 → AR 模型（系数 a[1..p]+噪声方差 σ²）。
     *
     * @param covariances [r0, r1..rp]（r0&gt;0）
     * @throws IllegalArgumentException null/长度不足/r0≤0
     */
    public static DurbinLevinson.ArModel solve(double[] covariances) {
        if (covariances == null || covariances.length < 2) {
            throw new IllegalArgumentException("自协方差须含 r0 与至少一阶（实际 "
                    + (covariances == null ? "null" : covariances.length) + "）");
        }
        double r0 = covariances[0];
        if (!(r0 > 0.0)) {
            throw new IllegalArgumentException("r0 为正（实际 " + r0 + "）");
        }
        double[] normalized = new double[covariances.length - 1];
        for (int i = 0; i < normalized.length; i++) {
            normalized[i] = covariances[i + 1] / r0;
        }
        DurbinLevinson.ArModel model = DurbinLevinson.solve(normalized);
        double variance = r0;
        for (int j = 0; j < model.coefficients().length; j++) {
            variance -= model.coefficients()[j] * covariances[j + 1];
        }
        return new DurbinLevinson.ArModel(model.coefficients(), variance);
    }
}
