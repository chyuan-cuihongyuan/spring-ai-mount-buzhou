package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Goertzel 单频检测（spec 11004 / Y11009 / impl 2457）——Goertzel 1958 思想
 * （DTMF/电话信令同源）：**二阶复数振子递推 w[n]=x[n]+2cosω·w[n−1]−w[n−2]
 * + 终值模平方读数 |X(f)|²**——免全谱 FFT 的 O(n) 单音响应面（非归一 DFT
 * 频仓模平方等价）。
 *
 * <p>f∈(0,fs) 开域（直流/奈奎斯特边界拒绝——递推系数退化面）；
 * null/非正采样率/频域越界/非有限值 fail-fast；复算确定。
 */
public final class GoertzelAlgorithm {

    private GoertzelAlgorithm() {
    }

    /**
     * 目标频率功率（|X(f)|²，非归一 DFT 频仓模平方）。
     *
     * @param samples 实采样
     * @param targetFrequency 目标频率（0&lt;f&lt;fs）
     * @param sampleRate 采样率（&gt;0）
     * @throws IllegalArgumentException null/空/非正采样率/频域越界/非有限值
     */
    public static double power(double[] samples, double targetFrequency,
            double sampleRate) {
        validate(samples, targetFrequency, sampleRate);
        double omega = 2.0 * Math.PI * targetFrequency / sampleRate;
        double coefficient = 2.0 * Math.cos(omega);
        double previous = 0.0;
        double beforePrevious = 0.0;
        for (double sample : samples) {
            double current = sample + coefficient * previous - beforePrevious;
            beforePrevious = previous;
            previous = current;
        }
        return previous * previous + beforePrevious * beforePrevious
                - coefficient * previous * beforePrevious;
    }

    private static void validate(double[] samples, double targetFrequency,
            double sampleRate) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("采样非空");
        }
        if (!(sampleRate > 0.0) || !Double.isFinite(sampleRate)) {
            throw new IllegalArgumentException("采样率正有限（实际 " + sampleRate + "）");
        }
        if (!(targetFrequency > 0.0) || targetFrequency >= sampleRate
                || !Double.isFinite(targetFrequency)) {
            throw new IllegalArgumentException("目标频域 (0,fs)（实际 " + targetFrequency
                    + "，fs=" + sampleRate + "）");
        }
        for (int i = 0; i < samples.length; i++) {
            if (!Double.isFinite(samples[i])) {
                throw new IllegalArgumentException("采样非有限（第 " + i + " 位 " + samples[i] + "）");
            }
        }
    }
}
