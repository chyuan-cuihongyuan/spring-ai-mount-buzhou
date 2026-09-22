package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 波动系数读面（spec 1902 / T3005 / impl 1503）——金融 CV（
 * coefficient of variation）语义：stddev/|mean| 无量纲化——绝对
 * 波动跨规模不可比（百元股 ±10 与十元股 ±10 是两回事），相对波动
 * 同一把尺分档（STABLE/MODERATE/VOLATILE）。
 *
 * <p>纯函数零状态；均值零时 CV 数学未定义——fail-fast 不哑算。
 */
public final class CoefficientOfVariation {

    private CoefficientOfVariation() {
    }

    /** 波动三档：STABLE → MODERATE → VOLATILE（金融惯例带，边界严格小于）。 */
    public enum Volatility { STABLE, MODERATE, VOLATILE }

    /**
     * 波动系数：stddev/|mean|。契约：stddev ≥ 0、mean ≠ 0
     * （fail-fast——分母为零时 CV 未定义）。
     */
    public static double cv(double mean, double stddev) {
        if (stddev < 0) {
            throw new IllegalArgumentException("stddev 不能为负：" + stddev);
        }
        if (mean == 0) {
            throw new IllegalArgumentException("mean 不能为 0（CV 未定义）");
        }
        return stddev / Math.abs(mean);
    }

    /**
     * 波动分档：cv &lt; 0.15 → STABLE；&lt; 0.5 → MODERATE；其余
     * VOLATILE（边界严格小于）。
     */
    public static Volatility band(double cv) {
        if (cv < 0.15) {
            return Volatility.STABLE;
        }
        if (cv < 0.5) {
            return Volatility.MODERATE;
        }
        return Volatility.VOLATILE;
    }
}
