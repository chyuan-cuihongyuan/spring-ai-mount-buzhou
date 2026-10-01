package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 解析信号包络（spec 10038 / X10077 / impl 2441）——Hilbert 1912/SciPy
 * hilbert 思想（HilbertCurve 已占异面：填充曲线）：**FFT 频域单边化（直流与
 * 奈奎斯特点保 1、正频 ×2、负频置 0）+逆变换取模**——实信号 z=x+jH(x) 的
 |z| 瞬时包络。消费 FftIterative（spec 10036）——流程内自组合。
 *
 * <p>2 的幂长约束沿 FFT 域；null/非 2 幂/非有限值 fail-fast；同输入复算确定。
 */
public final class HilbertTransform {

    private HilbertTransform() {
    }

    /**
     * 瞬时包络（解析信号逐点模长）。
     *
     * @param signal 实信号（长度 2 的幂且 ≥2）
     * @return 包络（与输入等长）
     * @throws IllegalArgumentException null/非 2 幂/非有限值
     */
    public static double[] envelope(double[] signal) {
        validate(signal);
        int n = signal.length;
        double[] re = signal.clone();
        double[] im = new double[n];
        FftIterative.transform(re, im);
        applySpectrumKernel(re, im, n);
        inverse(re, im, n);
        double[] envelope = new double[n];
        for (int i = 0; i < n; i++) {
            envelope[i] = Math.hypot(re[i], im[i]);
        }
        return envelope;
    }

    /** 单边化核：h[0]=h[n/2]=1、1≤k<n/2 ×2、k>n/2 置 0。 */
    private static void applySpectrumKernel(double[] re, double[] im, int n) {
        for (int k = 1; k < n / 2; k++) {
            re[k] *= 2.0;
            im[k] *= 2.0;
        }
        for (int k = n / 2 + 1; k < n; k++) {
            re[k] = 0.0;
            im[k] = 0.0;
        }
    }

    /** 共轭-正变换-共轭除 n 逆变换。 */
    private static void inverse(double[] re, double[] im, int n) {
        for (int i = 0; i < n; i++) {
            im[i] = -im[i];
        }
        FftIterative.transform(re, im);
        for (int i = 0; i < n; i++) {
            re[i] /= n;
            im[i] = -im[i] / n;
        }
    }

    private static void validate(double[] signal) {
        if (signal == null) {
            throw new IllegalArgumentException("信号非 null");
        }
        if (signal.length < 2 || Integer.bitCount(signal.length) != 1) {
            throw new IllegalArgumentException("长度为 2 的幂且 ≥2（实际 " + signal.length + "）");
        }
        for (int i = 0; i < signal.length; i++) {
            if (!Double.isFinite(signal[i])) {
                throw new IllegalArgumentException("采样非有限（第 " + i + " 位 " + signal[i] + "）");
            }
        }
    }
}
