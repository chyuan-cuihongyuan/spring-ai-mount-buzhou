package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 标量卡尔曼滤波（spec 4036 / T6073 / impl 2137）——单指标
 * 带不确定性闭环融合思想（标量 Kalman 预测/更新两步）：
 * 预测步常值模型状态外推不变、方差增长 {@code p += q}（过程
 * 噪声——不确定性随时间累积）；更新步增益 {@code k = p/(p+r)}
 * （测量噪声 r），状态融合 {@code x += k(z−x)}、方差收缩
 * {@code p *= (1−k)}——增益随确定性升高单调下降（愈发不信
 * 新读数），predict 后方差回升（重新信任新读数）——闭环
 * 自适应，与 EWMA 恒定权重平滑不同面。
 */
public final class ScalarKalmanFilter {

    private final double processNoise;
    private final double measurementNoise;
    private double state;
    private double variance;
    private double lastGain;

    /** 定构（q≥0、r>0、p0>0 否则 fail-fast；状态初值 x0）。 */
    public ScalarKalmanFilter(double initialState, double initialVariance,
            double processNoise, double measurementNoise) {
        if (processNoise < 0 || measurementNoise <= 0 || initialVariance <= 0) {
            throw new IllegalArgumentException("q≥0 / r>0 / p0>0："
                    + processNoise + "/" + measurementNoise + "/" + initialVariance);
        }
        this.state = initialState;
        this.variance = initialVariance;
        this.processNoise = processNoise;
        this.measurementNoise = measurementNoise;
        this.lastGain = initialVariance / (initialVariance + measurementNoise);
    }

    /** 预测步（常值模型：状态外推不变、方差增长——不确定性累积）。 */
    public void predict() {
        variance += processNoise;
    }

    /** 更新步（增益融合新读数；非有限读数 fail-fast）。 */
    public void update(double measurement) {
        if (!Double.isFinite(measurement)) {
            throw new IllegalArgumentException("读数需有限：" + measurement);
        }
        double gain = variance / (variance + measurementNoise);
        state += gain * (measurement - state);
        variance *= (1 - gain);
        lastGain = gain;
    }

    /** 融合读数（预测/更新两步合并的便捷口）。 */
    public void observe(double measurement) {
        predict();
        update(measurement);
    }

    /** 状态估计读数。 */
    public double state() {
        return state;
    }

    /** 估计方差读数（不确定性度量）。 */
    public double variance() {
        return variance;
    }

    /** 最近一次更新增益读数。 */
    public double lastGain() {
        return lastGain;
    }
}
