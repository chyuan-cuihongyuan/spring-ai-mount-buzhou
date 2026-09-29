package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * SAX 符号聚合近似（spec 8046 / V8091 / impl 2347）——
 * Lin 2003 SAX 思想（Keogh 族）：**z 归一 → 等宽 PAA 分段 →
 * 高斯分位断点符号化（alphabetSize=3 断点 ±0.43 等概率）**——
 * 原始浮点序列距离贵（相似检索放大）的病解。零方差序列诚实
 * 全中位符（z=0——勘误：初版注释误写首符——中位符钉住修正）；wordSize/字母表域 fail-fast；确定性纯函数。
 *
 * <p>与 PaaCodec（spec 8047）同族不同面：符号离散化 vs 分段
 * 均值连续降维。
 */
public final class SaxCodec {

    /** alphabetSize=3 的高斯分位断点（±0.43）。 */
    private static final double[] BREAKPOINTS = {-0.43, 0.43};
    private static final char FIRST_SYMBOL = 'a';
    private static final double EPSILON = 1e-12;

    private SaxCodec() {
    }

    /** 变换为 SAX 词（wordSize ≥1；null/空/越域 fail-fast）。 */
    public static String transform(double[] series, int wordSize) {
        if (series == null || series.length == 0) {
            throw new IllegalArgumentException("序列非空");
        }
        if (wordSize < 1 || wordSize > series.length) {
            throw new IllegalArgumentException("wordSize ∈[1,n]（实际 " + wordSize + "）");
        }
        double[] normalized = zNormalize(series);
        double[] reduced = PaaCodec.transform(normalized, wordSize);
        StringBuilder word = new StringBuilder();
        for (double value : reduced) {
            int symbol = 0;
            for (double breakpoint : BREAKPOINTS) {
                if (value > breakpoint) {
                    symbol++;
                }
            }
            word.append((char) (FIRST_SYMBOL + symbol));
        }
        return word.toString();
    }

    private static double[] zNormalize(double[] series) {
        double mean = 0;
        for (double value : series) {
            mean += value;
        }
        mean /= series.length;
        double variance = 0;
        for (double value : series) {
            variance += (value - mean) * (value - mean);
        }
        variance /= series.length;
        double stdDev = Math.sqrt(variance);
        double[] normalized = new double[series.length];
        if (stdDev < EPSILON) {
            java.util.Arrays.fill(normalized, 0);
            return normalized;
        }
        for (int i = 0; i < series.length; i++) {
            normalized[i] = (series[i] - mean) / stdDev;
        }
        return normalized;
    }
}
