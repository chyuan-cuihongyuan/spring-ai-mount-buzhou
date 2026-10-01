package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 不均匀采样频谱（spec 10040 / X10081 / impl 2443）——Lomb 1976/Scargle 1982
 * 思想（astro-Timeseries 同源）：**每频点 τ 相位偏移中心化
 * （τ=atan2(Σsin 2ωt, Σcos 2ωt)/2ω）+正余弦最小二乘能量 / 2σ² 归一**——
 * FFT 均匀采样前提不满足时的不规则观测频谱估计面。
 *
 * <p>σ² 取总体方差（1/N）；频域网格均匀扫频（正频域）。null/长度不配/
 * 样本不足/非有限值/非正频/越序频界 fail-fast；同输入复算确定。
 */
public final class LombScargle {

    private LombScargle() {
    }

    /**
     * Lomb–Scargle 周期图（频点升序，与请求网格一一对应）。
     *
     * @param times 观测时刻（任意正距）
     * @param values 观测值（与 times 等长）
     * @param minFrequency 扫频下界（>0）
     * @param maxFrequency 扫频上界（≥minFrequency）
     * @param frequencyCount 频点数（≥1）
     * @throws IllegalArgumentException null/长度不配/样本不足/非有限值/频域非法
     */
    public static double[] periodogram(double[] times, double[] values,
            double minFrequency, double maxFrequency, int frequencyCount) {
        validate(times, values, minFrequency, maxFrequency, frequencyCount);
        int n = values.length;
        double mean = 0;
        for (double v : values) {
            mean += v;
        }
        mean /= n;
        double variance = 0;
        for (double v : values) {
            variance += (v - mean) * (v - mean);
        }
        variance /= n;
        if (variance == 0.0) {
            throw new IllegalArgumentException("方差为零（常数信号频谱无定义）");
        }
        double[] power = new double[frequencyCount];
        for (int i = 0; i < frequencyCount; i++) {
            double frequency = minFrequency + (maxFrequency - minFrequency)
                    * i / Math.max(frequencyCount - 1, 1);
            power[i] = powerAt(times, values, mean, variance,
                    2.0 * Math.PI * frequency);
        }
        return power;
    }

    /** 单频点 Lomb–Scargle 功率。 */
    private static double powerAt(double[] times, double[] values, double mean,
            double variance, double omega) {
        double sinSum = 0;
        double cosSum = 0;
        for (double t : times) {
            sinSum += Math.sin(2.0 * omega * t);
            cosSum += Math.cos(2.0 * omega * t);
        }
        double tau = Math.atan2(sinSum, cosSum) / (2.0 * omega);
        double cosSquared = 0;
        double sinSquared = 0;
        double cosWeighted = 0;
        double sinWeighted = 0;
        for (int i = 0; i < values.length; i++) {
            double phase = omega * (times[i] - tau);
            double cos = Math.cos(phase);
            double sin = Math.sin(phase);
            double centered = values[i] - mean;
            cosSquared += cos * cos;
            sinSquared += sin * sin;
            cosWeighted += centered * cos;
            sinWeighted += centered * sin;
        }
        return (cosWeighted * cosWeighted / cosSquared
                + sinWeighted * sinWeighted / sinSquared) / (2.0 * variance);
    }

    private static void validate(double[] times, double[] values, double minFrequency,
            double maxFrequency, int frequencyCount) {
        if (times == null || values == null) {
            throw new IllegalArgumentException("数组非 null");
        }
        if (times.length != values.length) {
            throw new IllegalArgumentException("长度不配（times=" + times.length
                    + " values=" + values.length + "）");
        }
        if (values.length < 2) {
            throw new IllegalArgumentException("样本 ≥2（实际 " + values.length + "）");
        }
        if (frequencyCount < 1) {
            throw new IllegalArgumentException("频点数为正（实际 " + frequencyCount + "）");
        }
        if (!(minFrequency > 0.0)) {
            throw new IllegalArgumentException("扫频下界为正（实际 " + minFrequency + "）");
        }
        if (maxFrequency < minFrequency) {
            throw new IllegalArgumentException("频界越序（min=" + minFrequency
                    + " max=" + maxFrequency + "）");
        }
        for (int i = 0; i < values.length; i++) {
            if (!Double.isFinite(times[i]) || !Double.isFinite(values[i])) {
                throw new IllegalArgumentException("观测非有限（第 " + i + " 位）");
            }
        }
    }
}
