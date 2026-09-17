package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * lag-k 自相关（spec 3037 / T5075 / impl 2037）——时间序列 ACF
 * 思想（Box-Jenkins）：x 与自身滞后 k 平移的 Pearson 相关——
 * **周期性/趋势性/白噪性**的一把尺：lag=周期 → ~+1（周期信号
 * 自锁定）、趋势序列 lag 小即高正（惯性）、白噪各 lag ~0（无
 * 记忆）。「输出是否打转（周期）」「指标是否有惯性（趋势）」
 * 「扰动是否独立（白噪）」的判定地基。
 *
 * <p>纯函数静态件；零方差 NaN（诚实——常数列无相关可言）。
 */
public final class Autocorrelation {

    private Autocorrelation() {
    }

    /**
     * lag-k 自相关：x[0..n−k) 与 x[k..n) 的 Pearson 相关。
     *
     * @throws IllegalArgumentException lag ≥ n / lag &lt; 1 / null
     */
    public static double lagK(double[] series, int lag) {
        if (series == null || series.length < 2 || lag < 1 || lag >= series.length) {
            throw new IllegalArgumentException("series ≥ 2 元且 1 ≤ lag < n："
                    + (series == null ? "null" : series.length) + "/" + lag);
        }
        int pairs = series.length - lag;
        double meanX = 0;
        double meanY = 0;
        for (int i = 0; i < pairs; i++) {
            meanX += series[i];
            meanY += series[i + lag];
        }
        meanX /= pairs;
        meanY /= pairs;
        double covariance = 0;
        double varianceX = 0;
        double varianceY = 0;
        for (int i = 0; i < pairs; i++) {
            double dx = series[i] - meanX;
            double dy = series[i + lag] - meanY;
            covariance += dx * dy;
            varianceX += dx * dx;
            varianceY += dy * dy;
        }
        if (varianceX == 0 || varianceY == 0) {
            return Double.NaN;   // 常数段无相关可言
        }
        return covariance / Math.sqrt(varianceX * varianceY);
    }
}
