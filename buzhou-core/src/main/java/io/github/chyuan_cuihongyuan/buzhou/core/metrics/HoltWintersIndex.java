package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * Holt-Winters 季节指数（spec 4037 / T6075 / impl 2138）——
 * 周期信号的三参数加法指数平滑思想（statsmodels
 * ExponentialSmoothing）：水平/趋势/季节三项在线更新——
 * <pre>
 *   L_t = α(y_t − S_{t−m}) + (1−α)(L_{t−1} + b_{t−1})
 *   b_t = β(L_t − L_{t−1}) + (1−β) b_{t−1}
 *   S_t = γ(y_t − L_t) + (1−γ) S_{t−m}
 * </pre>
 * 预热启发式（statsmodels 同款）：恰两季预热——首季均值定
 * 水平、次季均值差定趋势、首季去均值定季节初值；预热未毕业
 * 诚实不猜（forecast/level ISE）。
 *
 * <p>与 {@link HoltForecaster} 同族不同面：水平+趋势 vs
 * 水平+趋势+季节指数。
 */
public final class HoltWintersIndex {

    private final int period;
    private final double alpha;
    private final double beta;
    private final double gamma;
    private final double[] warmup;
    private final double[] seasonal;
    private int warmupCount;
    private boolean warmedUp;
    private double level;
    private double trend;
    private long observed;

    /** 定构（period≥2、α/β/γ∈(0,1) 否则 fail-fast）。 */
    public HoltWintersIndex(int period, double alpha, double beta, double gamma) {
        if (period < 2 || alpha <= 0 || alpha >= 1 || beta <= 0 || beta >= 1
                || gamma <= 0 || gamma >= 1) {
            throw new IllegalArgumentException("period≥2 且 α/β/γ∈(0,1)："
                    + period + "/" + alpha + "/" + beta + "/" + gamma);
        }
        this.period = period;
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.warmup = new double[2 * period];
        this.seasonal = new double[period];
    }

    /** 逐位观测（前 2 季为预热缓冲，满即启发式初始化并开始在线更新）。 */
    public void observe(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("观测需有限：" + value);
        }
        if (!warmedUp) {
            warmup[warmupCount++] = value;
            if (warmupCount == warmup.length) {
                initialize();
            }
            return;
        }
        int slot = (int) (observed % period);
        double seasonalPrev = seasonal[slot];
        double previousLevel = level;
        level = alpha * (value - seasonalPrev) + (1 - alpha) * (previousLevel + trend);
        trend = beta * (level - previousLevel) + (1 - beta) * trend;
        seasonal[slot] = gamma * (value - level) + (1 - gamma) * seasonalPrev;
        observed++;
    }

    /** 向前 h 步预测（预热未毕业 ISE——未毕业诚实不猜）。 */
    public double forecast(int horizon) {
        requireWarmedUp();
        if (horizon < 1) {
            throw new IllegalArgumentException("horizon≥1：" + horizon);
        }
        int slot = (int) ((observed + horizon - 1) % period);
        return level + horizon * trend + seasonal[slot];
    }

    /** 水平读数。 */
    public double level() {
        requireWarmedUp();
        return level;
    }

    /** 趋势读数。 */
    public double trend() {
        requireWarmedUp();
        return trend;
    }

    /** 季节槽位指数读数（预热未毕业 ISE）。 */
    public double seasonalIndexOf(int slot) {
        requireWarmedUp();
        if (slot < 0 || slot >= period) {
            throw new IllegalArgumentException("slot∈[0,period)：" + slot);
        }
        return seasonal[slot];
    }

    /** 预热是否毕业（恰 2 季观测后真）。 */
    public boolean warmedUp() {
        return warmedUp;
    }

    /** 已毕业观测数读数（不含预热）。 */
    public long observedAfterWarmup() {
        return observed;
    }

    /** statsmodels 式启发式：首季均值定水平、季差定趋势、首季去均值定季节。 */
    private void initialize() {
        double firstSeasonMean = Arrays.stream(warmup, 0, period).average().orElse(0);
        double secondSeasonMean = Arrays.stream(warmup, period, 2 * period).average().orElse(0);
        for (int i = 0; i < period; i++) {
            seasonal[i] = warmup[i] - firstSeasonMean;
        }
        level = firstSeasonMean;
        trend = (secondSeasonMean - firstSeasonMean) / period;
        warmedUp = true;
    }

    private void requireWarmedUp() {
        if (!warmedUp) {
            throw new IllegalStateException("预热未毕业（需恰 2 季=" + warmup.length + " 观测），诚实不猜");
        }
    }
}
