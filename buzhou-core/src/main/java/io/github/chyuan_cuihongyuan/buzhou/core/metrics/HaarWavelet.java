package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Haar 小波全分解（spec 11003 / Y11007 / impl 2456）——Haar 1909 思想
 * （pywt/scipy 同源）：**对相邻对 (x₀+x₁)/√2、(x₀−x₁)/√2 得近似/细节，
 * 近似段逐层折半至单点**的正交归一多分辨分解——均值/差值的最简正交小波。
 * 输出树状布局 [最粗近似, 细节由粗到细...]；Parseval 严格守恒。
 *
 * <p>2 幂长 ≥2 约束；null/空/非 2 幂/非有限值 fail-fast；复算确定。
 */
public final class HaarWavelet {

    /** 正交归一因子 1/√2。 */
    private static final double INVERSE_SQRT_TWO = 1.0 / Math.sqrt(2.0);

    private HaarWavelet() {
    }

    /**
     * 正交 Haar 全分解。
     *
     * @param x 实信号（长度 2 的幂且 ≥2）
     * @return 系数（[最粗近似, 细节由粗到细...]，与输入等长）
     * @throws IllegalArgumentException null/空/非 2 幂/非有限值
     */
    public static double[] forward(double[] x) {
        validate(x);
        double[] active = x.clone();
        int length = x.length;
        List<double[]> details = new ArrayList<>();
        while (length > 1) {
            int half = length / 2;
            double[] approximation = new double[half];
            double[] detail = new double[half];
            for (int i = 0; i < half; i++) {
                double a = active[2 * i];
                double b = active[2 * i + 1];
                approximation[i] = (a + b) * INVERSE_SQRT_TWO;
                detail[i] = (a - b) * INVERSE_SQRT_TWO;
            }
            details.add(detail);
            active = approximation;
            length = half;
        }
        double[] coefficients = new double[x.length];
        coefficients[0] = active[0];
        int position = 1;
        for (int level = details.size() - 1; level >= 0; level--) {
            double[] detail = details.get(level);
            System.arraycopy(detail, 0, coefficients, position, detail.length);
            position += detail.length;
        }
        return coefficients;
    }

    private static void validate(double[] x) {
        if (x == null || x.length == 0) {
            throw new IllegalArgumentException("信号非空");
        }
        if (Integer.bitCount(x.length) != 1) {
            throw new IllegalArgumentException("长度为 2 的幂（实际 " + x.length + "）");
        }
        for (int i = 0; i < x.length; i++) {
            if (!Double.isFinite(x[i])) {
                throw new IllegalArgumentException("采样非有限（第 " + i + " 位 " + x[i] + "）");
            }
        }
    }
}
