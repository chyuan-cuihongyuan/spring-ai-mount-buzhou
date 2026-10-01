package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 实倒频谱（spec 11007 / Y11015 / impl 2460）——Bogert–Healy–Tukey 1963 思想
 * （语音同源）：**FFT → 逐仓 log(mag+ε) → IFFT 取实部**——频谱周期结构的
 * quefrency 域读出面（ε 防零幅对数爆炸）。消费 FftIterative（spec 10036）——
 * 流程内自组合。
 *
 * <p>2 幂长 ≥2 约束；null/非 2 幂/非有限值 fail-fast；复算确定。
 */
public final class Cepstrum {

    /** 零幅防爆炸项。 */
    private static final double ZERO_GUARD = 1e-12;

    private Cepstrum() {
    }

    /**
     * 实倒频谱。
     *
     * @param x 实信号（长度 2 的幂且 ≥2）
     * @return 倒谱（与输入等长）
     * @throws IllegalArgumentException null/非 2 幂/非有限值
     */
    public static double[] realCepstrum(double[] x) {
        validate(x);
        int n = x.length;
        double[] re = x.clone();
        double[] im = new double[n];
        FftIterative.transform(re, im);
        for (int k = 0; k < n; k++) {
            double magnitude = Math.hypot(re[k], im[k]);
            re[k] = Math.log(magnitude + ZERO_GUARD);
            im[k] = 0.0;
        }
        inverse(re, im, n);
        return re;
    }

    /** 共轭-正变换-共轭除 n 逆变换（实部留在 re）。 */
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

    private static void validate(double[] x) {
        if (x == null) {
            throw new IllegalArgumentException("信号非 null");
        }
        if (x.length < 2 || Integer.bitCount(x.length) != 1) {
            throw new IllegalArgumentException("长度为 2 的幂且 ≥2（实际 " + x.length + "）");
        }
        for (int i = 0; i < x.length; i++) {
            if (!Double.isFinite(x[i])) {
                throw new IllegalArgumentException("采样非有限（第 " + i + " 位 " + x[i] + "）");
            }
        }
    }
}
