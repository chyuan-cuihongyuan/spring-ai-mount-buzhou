package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Croston 间歇需求预测器（spec 3034 / T5069 / impl 2035）——
 * Croston 1972 思想：稀疏序列（大量零+偶发需求——工具调用/审计
 * 事件/故障上报）朴素指数平滑会被零海拖向零；Croston 把**需求
 * 大小 z** 与**需求间隔 p** 分开平滑（仅在非零观测时更新），预测
 * 率 = z'/p'——「毛刺多大不重要，率才是口径」（每周期期望需求
 * 量）。间歇型序列短程外推的地基件。
 *
 * <p>首个非零初始化（z'=z、p'=实测间隔）；无非零观测 forecast
 * NaN（诚实——零海无基准可言）；非负需求校验；非线程安全。
 */
public final class CrostonForecaster {

    private final double alpha;
    private double demandSize = Double.NaN;
    private double demandInterval = Double.NaN;
    private long periodsSinceNonzero;
    private long observations;
    private long nonzeroCount;

    /** 平滑率 α∈(0,1)。 */
    public CrostonForecaster(double alpha) {
        if (!(alpha > 0 && alpha < 1)) {
            throw new IllegalArgumentException("alpha∈(0,1)：" + alpha);
        }
        this.alpha = alpha;
    }

    /**
     * 周期观测（demand ≥ 0；零=无需求只计间隔；非零=更新双分量）。
     */
    public void observe(double demand) {
        if (!(demand >= 0)) {
            throw new IllegalArgumentException("demand ≥ 0：" + demand);
        }
        observations++;
        periodsSinceNonzero++;
        if (demand > 0) {
            double interval = periodsSinceNonzero;
            if (Double.isNaN(demandSize)) {
                demandSize = demand;
                demandInterval = interval;
            } else {
                demandSize = alpha * demand + (1 - alpha) * demandSize;
                demandInterval = alpha * interval + (1 - alpha) * demandInterval;
            }
            periodsSinceNonzero = 0;
            nonzeroCount++;
        }
    }

    /** 每周期期望需求率 z'/p'（无非零观测 NaN）。 */
    public double forecastPerPeriod() {
        return Double.isNaN(demandSize) ? Double.NaN : demandSize / demandInterval;
    }

    /** 平滑后需求大小分量 z'。 */
    public double demandSize() {
        return demandSize;
    }

    /** 平滑后需求间隔分量 p'。 */
    public double demandInterval() {
        return demandInterval;
    }

    /** 周期观测总数。 */
    public long observations() {
        return observations;
    }

    /** 非零观测数（稀疏度对账面：nonzero/observations）。 */
    public long nonzeroCount() {
        return nonzeroCount;
    }
}
