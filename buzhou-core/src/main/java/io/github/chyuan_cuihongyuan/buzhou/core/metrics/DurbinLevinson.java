package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Durbin–Levinson AR 递推（spec 11010 / Y11021 / impl 2463）——Levinson 1947/
 * Durbin 1960 思想（Statsmodels 同源）：**E(0)=1 起逐阶递推——反射系数
 * k_k=(r_k−Σa_j r_{k−j})/E_{k−1}、系数对称镜像更新、E_k=E_{k−1}(1−k_k²)**
 * ——Toeplitz 线性系统的 O(p²) 递推解（AR 建模基座）。
 *
 * <p>r 域 (−1,1) 开域（|r1|≥1 非平稳拒绝）；null/空 fail-fast；ArModel
 * 嵌套 record；复算确定。
 */
public final class DurbinLevinson {

    private DurbinLevinson() {
    }

    /** AR 模型（系数 a[1..p] + 预测误差方差）。 */
    public record ArModel(double[] coefficients, double errorVariance) {
    }

    /**
     * 自相关序列 → AR 模型。
     *
     * @param autocorrelations [r1..rp]（r 域 (−1,1)）
     * @throws IllegalArgumentException null/空/|r|≥1
     */
    public static ArModel solve(double[] autocorrelations) {
        if (autocorrelations == null || autocorrelations.length == 0) {
            throw new IllegalArgumentException("自相关序列非空");
        }
        int p = autocorrelations.length;
        double[] a = new double[p];
        double errorVariance = 1.0;
        for (int k = 1; k <= p; k++) {
            double r = autocorrelations[k - 1];
            if (Math.abs(r) >= 1.0) {
                throw new IllegalArgumentException("|r|<1（第 " + k + " 阶实际 " + r + "）");
            }
            double accumulator = r;
            for (int j = 1; j < k; j++) {
                accumulator -= a[j - 1] * autocorrelations[k - j - 1];
            }
            double reflection = accumulator / errorVariance;
            double[] updated = a.clone();
            updated[k - 1] = reflection;
            for (int j = 1; j < k; j++) {
                updated[j - 1] = a[j - 1] - reflection * a[k - j - 1];
            }
            a = updated;
            errorVariance *= (1.0 - reflection * reflection);
        }
        return new ArModel(a, errorVariance);
    }
}
