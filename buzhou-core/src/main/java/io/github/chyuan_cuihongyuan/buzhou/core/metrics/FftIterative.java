package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 迭代快速傅里叶（spec 10036 / X10073 / impl 2439）——Cooley–Tukey 1965 思想
 * （SciPy fft/NumPy 同源）：**位反转置换重排+自底向上蝶形（子宽度 1 起逐层
 * 倍增，twiddle 旋转因子直接三角计算）**——DFT O(n²) 直接法的 O(n log n)
 * 加速基座。原位变换（re/im 双实数组）；2 的幂长约束（混合基/Bluestein
 * 非幂长另立）。
 *
 * <p>null 数组/长度不配/非 2 幂长/非有限值 fail-fast；同输入逐位复算确定。
 */
public final class FftIterative {

    private FftIterative() {
    }

    /**
     * 原位基-2 DIT 变换（频域写入 re/im）。
     *
     * @param re 实部（长度 2 的幂）
     * @param im 虚部（与 re 等长）
     * @throws IllegalArgumentException null 数组/长度不配/非 2 幂/非有限值
     */
    public static void transform(double[] re, double[] im) {
        validate(re, im);
        int n = re.length;
        bitReversePermutation(re, im, n);
        for (int width = 2; width <= n; width <<= 1) {
            double angle = -2.0 * Math.PI / width;
            double wRe = Math.cos(angle);
            double wIm = Math.sin(angle);
            for (int start = 0; start < n; start += width) {
                double curRe = 1.0;
                double curIm = 0.0;
                for (int offset = 0; offset < width / 2; offset++) {
                    int even = start + offset;
                    int odd = even + width / 2;
                    double oddRe = re[odd] * curRe - im[odd] * curIm;
                    double oddIm = re[odd] * curIm + im[odd] * curRe;
                    re[odd] = re[even] - oddRe;
                    im[odd] = im[even] - oddIm;
                    re[even] += oddRe;
                    im[even] += oddIm;
                    double nextRe = curRe * wRe - curIm * wIm;
                    curIm = curRe * wIm + curIm * wRe;
                    curRe = nextRe;
                }
            }
        }
    }

    /**
     * 实信号幅度谱（|X[k]|，长度 = 2 的幂采样数）。
     *
     * @throws IllegalArgumentException null/空/非 2 幂/非有限值
     */
    public static double[] magnitudes(double[] samples) {
        validateSingle(samples);
        double[] re = samples.clone();
        double[] im = new double[samples.length];
        transform(re, im);
        double[] magnitudes = new double[samples.length];
        for (int k = 0; k < samples.length; k++) {
            magnitudes[k] = Math.hypot(re[k], im[k]);
        }
        return magnitudes;
    }

    private static void validate(double[] re, double[] im) {
        if (re == null || im == null) {
            throw new IllegalArgumentException("数组非 null");
        }
        if (re.length != im.length) {
            throw new IllegalArgumentException("长度不配（re=" + re.length
                    + " im=" + im.length + "）");
        }
        requirePowerOfTwo(re.length);
        for (int i = 0; i < re.length; i++) {
            requireFinite(re[i], i);
            requireFinite(im[i], i);
        }
    }

    private static void validateSingle(double[] samples) {
        if (samples == null) {
            throw new IllegalArgumentException("信号非 null");
        }
        requirePowerOfTwo(samples.length);
        for (int i = 0; i < samples.length; i++) {
            requireFinite(samples[i], i);
        }
    }

    private static void requirePowerOfTwo(int length) {
        if (length < 2 || Integer.bitCount(length) != 1) {
            throw new IllegalArgumentException("长度为 2 的幂且 ≥2（实际 " + length + "）");
        }
    }

    private static void requireFinite(double value, int index) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("采样非有限（第 " + index + " 位 " + value + "）");
        }
    }

    /** 位反转置换（r 初值右移累进——O(n) 链式口径）。 */
    private static void bitReversePermutation(double[] re, double[] im, int n) {
        for (int i = 1, j = 0; i < n; i++) {
            int half = n >> 1;
            for (; j >= half; half >>= 1) {
                j -= half;
            }
            j += half;
            if (i < j) {
                double tmp = re[i];
                re[i] = re[j];
                re[j] = tmp;
                tmp = im[i];
                im[i] = im[j];
                im[j] = tmp;
            }
        }
    }
}
