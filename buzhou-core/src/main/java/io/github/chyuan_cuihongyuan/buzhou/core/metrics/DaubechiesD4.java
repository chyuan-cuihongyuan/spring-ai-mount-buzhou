package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Daubechies D4 小波全分解（spec 11006 / Y11013 / impl 2459）——Daubechies
 * 1988 思想（pywt 同源；HaarWavelet 已占异面：四系数紧支 vs 双系数分段常值）：
 * **标准四系数 c0..c3（(1±√3)/(4√2) 族）周期延拓滤波+下采样**——紧支正交
 * 小波（双消失矩：常量与线性信号的细节全零）。输出树状布局 [最粗近似,
 * 细节由粗到细...]；Parseval 严格守恒。
 *
 * <p>2 幂长 ≥4 约束（单层滤波窗长 4）；null/空/非 2 幂/非有限值 fail-fast；
 * 复算确定。
 */
public final class DaubechiesD4 {

    private static final double SQRT_THREE = Math.sqrt(3.0);
    private static final double C0 = (1.0 + SQRT_THREE) / (4.0 * Math.sqrt(2.0));
    private static final double C1 = (3.0 + SQRT_THREE) / (4.0 * Math.sqrt(2.0));
    private static final double C2 = (3.0 - SQRT_THREE) / (4.0 * Math.sqrt(2.0));
    private static final double C3 = (1.0 - SQRT_THREE) / (4.0 * Math.sqrt(2.0));

    private DaubechiesD4() {
    }

    /**
     * 正交 D4 全分解（周期延拓）。
     *
     * @param x 实信号（长度 2 的幂且 ≥4）
     * @return 系数（[最粗近似, 细节由粗到细...]，与输入等长）
     * @throws IllegalArgumentException null/空/非 2 幂/&lt;4/非有限值
     */
    public static double[] forward(double[] x) {
        validate(x);
        double[] active = x.clone();
        int length = x.length;
        List<double[]> details = new ArrayList<>();
        while (length >= 4) {
            int half = length / 2;
            double[] approximation = new double[half];
            double[] detail = new double[half];
            for (int i = 0; i < half; i++) {
                int second = (2 * i + 2) % length;
                int third = (2 * i + 3) % length;
                approximation[i] = C0 * active[2 * i] + C1 * active[2 * i + 1]
                        + C2 * active[second] + C3 * active[third];
                detail[i] = C3 * active[2 * i] - C2 * active[2 * i + 1]
                        + C1 * active[second] - C0 * active[third];
            }
            details.add(detail);
            active = approximation;
            length = half;
        }
        double[] coefficients = new double[x.length];
        System.arraycopy(active, 0, coefficients, 0, active.length);
        int position = active.length;
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
        if (x.length < 4 || Integer.bitCount(x.length) != 1) {
            throw new IllegalArgumentException("长度为 2 的幂且 ≥4（实际 " + x.length + "）");
        }
        for (int i = 0; i < x.length; i++) {
            if (!Double.isFinite(x[i])) {
                throw new IllegalArgumentException("采样非有限（第 " + i + " 位 " + x[i] + "）");
            }
        }
    }
}
