package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Holt 双参数指数平滑预测器（spec 3021 / T5043 / impl 2022）——
 * Charles Holt 1957 思想：**水平 + 趋势**双分量指数平滑——EWMA
 * 只跟水平（无趋势外推），Holt 增设趋势分量 b：预测 t+h =
 * level + h·trend。趋势型序列（增长的成本/延迟爬升/累积用量）
 * 的短程外推件；α 水平滑率 / β 趋势滑率（越大越贴新息）。
 *
 * <p>首个观测初始化（level=x₀、trend=0）；无观测 forecast NaN
 * （诚实边界）；非线程安全（单流口径）。
 */
public final class HoltForecaster {

    private final double alpha;
    private final double beta;
    private double level = Double.NaN;
    private double trend;
    private long observations;

    /** α/β ∈ (0,1)（水平/趋势平滑率，越大越贴新息）。 */
    public HoltForecaster(double alpha, double beta) {
        requireRate(alpha, "alpha");
        requireRate(beta, "beta");
        this.alpha = alpha;
        this.beta = beta;
    }

    /** 吸收一个观测：水平/趋势双递推。 */
    public void observe(double x) {
        if (observations == 0) {
            level = x;
            trend = 0;
        } else {
            double previousLevel = level;
            level = alpha * x + (1 - alpha) * (level + trend);
            trend = beta * (level - previousLevel) + (1 - beta) * trend;
        }
        observations++;
    }

    /** 外推 h 步：level + h·trend（h=0 即当前水平；无观测 NaN）。 */
    public double forecast(int steps) {
        if (steps < 0) {
            throw new IllegalArgumentException("steps ≥ 0：" + steps);
        }
        return observations == 0 ? Double.NaN : level + steps * trend;
    }

    /** 当前水平分量。 */
    public double level() {
        return level;
    }

    /** 当前趋势分量（每步增量）。 */
    public double trend() {
        return trend;
    }

    /** 已吸收观测数。 */
    public long observations() {
        return observations;
    }

    private static void requireRate(double rate, String name) {
        if (!(rate > 0 && rate < 1)) {
            throw new IllegalArgumentException(name + "∈(0,1)：" + rate);
        }
    }
}
