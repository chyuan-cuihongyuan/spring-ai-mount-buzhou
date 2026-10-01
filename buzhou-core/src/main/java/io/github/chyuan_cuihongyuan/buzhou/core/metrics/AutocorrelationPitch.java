package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 自相关基音检测（spec 11009 / Y11019 / impl 2462）——Rabiner 1972 思想
 * （语音基音检测同源）：**滞后域 [fs/fMax, fs/fMin] 扫归一化自相关
 * r(τ)=Σx[i]x[i+τ]/(窗口能量几何归一)，峰值 lag→fs/lag**——时域周期
 * 提取面（Cepstrum 的时域镜像）。
 *
 * <p>时长须 ≥2·maxLag+1；倍频程歧义消解（近峰 1% 容差内取最小滞后）；
 * null/非正采样率/频域反序/越界/时长不足 fail-fast；复算确定。
 */
public final class AutocorrelationPitch {

    /** 近峰容差（倍频程歧义消解——归一化相关 1%）。 */
    private static final double NEAR_PEAK_TOLERANCE = 0.01;

    private AutocorrelationPitch() {
    }

    /**
     * 基音估计（Hz）。
     *
     * @param samples 实采样
     * @param sampleRate 采样率（&gt;0）
     * @param minFrequency 搜索下界（&lt;maxFrequency）
     * @param maxFrequency 搜索上界（&lt;sampleRate/2）
     * @throws IllegalArgumentException null/非法采样率/频域反序/时长不足
     */
    public static double detectPitch(double[] samples, double sampleRate,
            double minFrequency, double maxFrequency) {
        validate(samples, sampleRate, minFrequency, maxFrequency);
        int minLag = (int) Math.floor(sampleRate / maxFrequency);
        int maxLag = (int) Math.ceil(sampleRate / minFrequency);
        double zeroLagEnergy = energy(samples, 0, samples.length - maxLag);
        if (zeroLagEnergy == 0.0) {
            return 0.0;
        }
        double bestCorrelation = Double.NEGATIVE_INFINITY;
        for (int lag = minLag; lag <= maxLag; lag++) {
            int overlap = samples.length - lag;
            double cross = 0.0;
            double windowEnergy = 0.0;
            for (int i = 0; i < overlap; i++) {
                cross += samples[i] * samples[i + lag];
                windowEnergy += samples[i] * samples[i];
            }
            if (windowEnergy == 0.0) {
                continue;
            }
            bestCorrelation = Math.max(bestCorrelation, cross / windowEnergy);
        }
        if (bestCorrelation == Double.NEGATIVE_INFINITY) {
            return 0.0;
        }
        // 倍频程歧义消解：纯周期信号 2 倍周期滞后同峰——取近峰最小滞后
        // （5% 容差内首个；口径沿经典 ACF 基音消歧）
        for (int lag = minLag; lag <= maxLag; lag++) {
            int overlap = samples.length - lag;
            double cross = 0.0;
            double windowEnergy = 0.0;
            for (int i = 0; i < overlap; i++) {
                cross += samples[i] * samples[i + lag];
                windowEnergy += samples[i] * samples[i];
            }
            if (windowEnergy == 0.0) {
                continue;
            }
            if (cross / windowEnergy >= bestCorrelation - NEAR_PEAK_TOLERANCE) {
                return sampleRate / lag;
            }
        }
        return 0.0;
    }

    private static double energy(double[] samples, int from, int length) {
        double sum = 0.0;
        for (int i = from; i < from + length; i++) {
            sum += samples[i] * samples[i];
        }
        return sum;
    }

    private static void validate(double[] samples, double sampleRate,
            double minFrequency, double maxFrequency) {
        if (samples == null) {
            throw new IllegalArgumentException("采样非 null");
        }
        if (!(sampleRate > 0.0) || !Double.isFinite(sampleRate)) {
            throw new IllegalArgumentException("采样率正有限（实际 " + sampleRate + "）");
        }
        if (!(minFrequency > 0.0) || minFrequency >= maxFrequency
                || maxFrequency >= sampleRate / 2.0) {
            throw new IllegalArgumentException("频域 0<fMin<fMax<fs/2（实际 "
                    + minFrequency + "," + maxFrequency + "，fs=" + sampleRate + "）");
        }
        int maxLag = (int) Math.ceil(sampleRate / minFrequency);
        if (samples.length < 2 * maxLag + 1) {
            throw new IllegalArgumentException("时长不足（需 ≥2·maxLag+1="
                    + (2 * maxLag + 1) + "，实际 " + samples.length + "）");
        }
        for (double v : samples) {
            if (!Double.isFinite(v)) {
                throw new IllegalArgumentException("采样非有限");
            }
        }
    }
}
