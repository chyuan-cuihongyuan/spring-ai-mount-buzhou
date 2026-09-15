package io.github.chyuan_cuihongyuan.buzhou.core.budget;

/**
 * 预算花费匀速曲线（spec 1819 / T2839 / impl 1420）——广告投放 spend
 * pacing 思想：预算周期内**匀速**是健康基线（target = 预算 × 已过时间比），
 * 花费对基线的偏离分三态——OVER（超前烧钱，周期末提前断粮）/ UNDER
 * （落后保守，预算用不完=价值漏损）/ ON（带内）。run-rate（运行率 =
 * 花费比/时间比）回答「按当前速度期末会烧到几倍」。
 *
 * <p>纯函数零状态、只判态不干预（限流动作归宿主）。
 */
public final class BudgetPacingCurve {

    /** 浮点噪声免疫宽限（0.55−0.5 之类的二进制尾差不得翻转边界判定）。 */
    private static final double FLOAT_EPSILON = 1e-12;

    private BudgetPacingCurve() {
    }

    /** 花费节奏三态：ON_PACE 带内 / OVER_PACING 超前 / UNDER_PACING 落后。 */
    public enum Pacing {

        /** 花费在目标带内（|偏离| ≤ 容差）。 */
        ON_PACE,

        /** 超前烧钱——周期末提前断粮风险。 */
        OVER_PACING,

        /** 落后保守——预算用不完的价值漏损。 */
        UNDER_PACING
    }

    /**
     * @param elapsedFraction 已过时间比 [0,1]
     * @param spentFraction   已花费比 [0,1]
     * @param targetFraction  目标花费比 = elapsedFraction
     * @param pacing          节奏三态
     */
    public record PacingReport(double elapsedFraction, double spentFraction,
                               double targetFraction, Pacing pacing) {

        /** 偏离 = 花费比 − 目标比（正=超前）。 */
        public double deviation() {
            return spentFraction - targetFraction;
        }

        /** 运行率 = spent/elapsed（时间过 0 -1 哨兵；期末=1 即恰好匀速）。 */
        public double runRate() {
            return elapsedFraction <= 0 ? -1d : spentFraction / elapsedFraction;
        }
    }

    /**
     * 判态入口。契约：elapsedFraction/spentFraction ∈ [0,1] 非 NaN、
     * tolerance ≥ 0（fail-fast）；语义：|spent − elapsed| ≤ tolerance 即
     * ON_PACE（边界含），高于即 OVER、低于即 UNDER。
     */
    public static PacingReport evaluate(double elapsedFraction, double spentFraction,
                                        double tolerance) {
        validateFraction("elapsedFraction", elapsedFraction);
        validateFraction("spentFraction", spentFraction);
        if (Double.isNaN(tolerance) || tolerance < 0) {
            throw new IllegalArgumentException("tolerance 须 ≥ 0 非 NaN：" + tolerance);
        }
        double deviation = spentFraction - elapsedFraction;
        Pacing pacing = deviation > tolerance + FLOAT_EPSILON ? Pacing.OVER_PACING
                : deviation < -tolerance - FLOAT_EPSILON ? Pacing.UNDER_PACING
                : Pacing.ON_PACE;
        return new PacingReport(elapsedFraction, spentFraction, elapsedFraction, pacing);
    }

    private static void validateFraction(String name, double value) {
        if (Double.isNaN(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(name + " 须在 [0,1] 非 NaN：" + value);
        }
    }
}
