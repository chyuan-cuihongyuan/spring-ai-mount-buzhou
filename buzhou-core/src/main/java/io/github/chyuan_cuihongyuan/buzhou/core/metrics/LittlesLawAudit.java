package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 利特尔法则一致性审计（spec 1856 / T2913 / impl 1457）——排队论
 * Little's Law（L = λ × W）思想：稳态下系统内平均数（并发/队列深）、
 * 到达率、平均逗留时间三者**必然互证**——任两者推出第三者，与实测第三
 * 者的偏差要么是计量失真（三处仪表至少一处错）要么是稳态假设破（突发/
 * 积压未消化）——两种情况都值得报。指标互证比单指标自证可信一个量级。
 *
 * <p>纯函数零状态、只审计不归因（失真定位归宿主）。
 */
public final class LittlesLawAudit {

    private LittlesLawAudit() {
    }

    /** 一致性两态：CONSISTENT 互证成立 / DIVERGENT 偏差超容差。 */
    public enum Consistency {

        /** L 与 λW 在容差内互证——仪表与稳态假设同可信。 */
        CONSISTENT,

        /** 偏差超容差——计量失真或稳态破（突发/积压）。 */
        DIVERGENT
    }

    /**
     * 隐含并发 L = λ × W。契约：arrivalRatePerSec ≥ 0、sojournMillis ≥ 0
     *（fail-fast）；单位换算（毫秒→秒）内置。
     */
    public static double impliedConcurrency(double arrivalRatePerSec,
                                            double sojournMillis) {
        validateNonNegative("arrivalRatePerSec", arrivalRatePerSec);
        validateNonNegative("sojournMillis", sojournMillis);
        return arrivalRatePerSec * sojournMillis / 1000.0d;
    }

    /**
     * 一致性审计。契约：measuredConcurrency ≥ 0、toleranceRatio ≥ 0
     *（fail-fast）；语义：|L − λW| ≤ tolerance × max(|λW|, 1) 即
     * CONSISTENT（零基线退化绝对口径——同 WindowShiftDetector 惯例）。
     */
    public static Consistency consistency(double measuredConcurrency,
                                          double arrivalRatePerSec,
                                          double sojournMillis,
                                          double toleranceRatio) {
        validateNonNegative("measuredConcurrency", measuredConcurrency);
        validateNonNegative("toleranceRatio", toleranceRatio);
        double implied = impliedConcurrency(arrivalRatePerSec, sojournMillis);
        double divergence = Math.abs(measuredConcurrency - implied);
        double denominator = Math.max(Math.abs(implied), 1.0d);
        return divergence <= toleranceRatio * denominator
                ? Consistency.CONSISTENT
                : Consistency.DIVERGENT;
    }

    private static void validateNonNegative(String name, double value) {
        if (Double.isNaN(value) || value < 0) {
            throw new IllegalArgumentException(name + " 须 ≥ 0 非 NaN：" + value);
        }
    }
}
