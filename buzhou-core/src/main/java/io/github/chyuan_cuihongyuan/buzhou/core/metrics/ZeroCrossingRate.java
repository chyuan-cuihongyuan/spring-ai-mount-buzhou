package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 过零率（spec 11008 / Y11017 / impl 2461）——语音/MIR 特征惯例（librosa
 * zero_crossing_rate 同源）：**相邻符号变化计数 /(n−1)**——最廉价的频率/
 * 噪声度特征面。零值沿用前有效符号（不构成符号翻转——口径明示）。
 *
 * <p>null/空/单点/非有限值 fail-fast；复算确定。
 */
public final class ZeroCrossingRate {

    private ZeroCrossingRate() {
    }

    /**
     * 过零率（[0,1]）。
     *
     * @param samples 实采样（长度 ≥2）
     * @throws IllegalArgumentException null/长度不足/非有限值
     */
    public static double rate(double[] samples) {
        if (samples == null || samples.length < 2) {
            throw new IllegalArgumentException("采样 ≥2（实际 "
                    + (samples == null ? "null" : samples.length) + "）");
        }
        for (int i = 0; i < samples.length; i++) {
            if (!Double.isFinite(samples[i])) {
                throw new IllegalArgumentException("采样非有限（第 " + i + " 位 " + samples[i] + "）");
            }
        }
        int crossings = 0;
        int previousSign = signAt(samples, 0);
        for (int i = 1; i < samples.length; i++) {
            int sign = signAt(samples, i);
            if (sign != 0 && previousSign != 0 && sign != previousSign) {
                crossings++;
            }
            if (sign != 0) {
                previousSign = sign;
            }
        }
        return (double) crossings / (samples.length - 1);
    }

    /** 符号读数（零返回 0——沿用前符号口径）。 */
    private static int signAt(double[] samples, int index) {
        if (samples[index] > 0.0) {
            return 1;
        }
        return samples[index] < 0.0 ? -1 : 0;
    }
}
