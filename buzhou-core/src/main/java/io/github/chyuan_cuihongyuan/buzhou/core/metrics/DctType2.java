package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 正交 DCT-II 变换（spec 10037 / X10075 / impl 2440）——Ahmed–Natarajan–Rao
 * 1974 思想（JPEG/MP3 同源）：**X[0]=√(1/N)·Σx（均衡尺度）、
 * X[k]=√(2/N)·Σ x[n]·cos(π(2n+1)k/(2N))**——正交归一口径（Parseval 严格
 * 守恒），JPEG 8 点块 1D 原语。任意长度 ≥1 直接 O(n²) 法。
 *
 * <p>null/空/非有限值 fail-fast；同输入逐位复算确定。
 */
public final class DctType2 {

    private DctType2() {
    }

    /**
     * 正交归一 DCT-II 变换。
     *
     * @param x 实信号（长度 ≥1）
     * @return DCT 系数（与输入等长）
     * @throws IllegalArgumentException null/空/非有限值
     */
    public static double[] transform(double[] x) {
        if (x == null || x.length == 0) {
            throw new IllegalArgumentException("信号非空");
        }
        int n = x.length;
        for (int i = 0; i < n; i++) {
            if (!Double.isFinite(x[i])) {
                throw new IllegalArgumentException("采样非有限（第 " + i + " 位 " + x[i] + "）");
            }
        }
        double[] coefficients = new double[n];
        for (int k = 0; k < n; k++) {
            double sum = 0.0;
            for (int t = 0; t < n; t++) {
                sum += x[t] * Math.cos(Math.PI * (2.0 * t + 1.0) * k / (2.0 * n));
            }
            double scale = k == 0 ? Math.sqrt(1.0 / n) : Math.sqrt(2.0 / n);
            coefficients[k] = scale * sum;
        }
        return coefficients;
    }
}
