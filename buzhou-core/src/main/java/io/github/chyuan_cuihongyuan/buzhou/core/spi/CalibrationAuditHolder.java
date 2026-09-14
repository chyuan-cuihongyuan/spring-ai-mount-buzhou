package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.Optional;

/**
 * Token 校准审计 Holder（spec 1618 / T2387，spec 819 孤类接线）：
 * TokenBudgetHook.afterModel 同点对账（CharHeuristic 估算 prompt vs 模型回报
 * usage.promptTokens）经此登记读出——「预算按估算设、账单按真实来」的系统性
 * 偏差从感觉变数字。
 * @since 1.0.0
 */
public final class CalibrationAuditHolder {

    private static volatile EstimatorCalibrationAudit audit = new EstimatorCalibrationAudit();

    private CalibrationAuditHolder() {
    }

    /** 替换实例（测试；null = 重置）。 */
    public static void install(EstimatorCalibrationAudit instance) {
        audit = instance == null ? new EstimatorCalibrationAudit() : instance;
    }

    /** 当前审计器。 */
    public static EstimatorCalibrationAudit audit() {
        return audit;
    }

    /** 校准读数便捷面。 */
    public static EstimatorCalibrationAudit.Calibration calibration() {
        return audit().audit();
    }

    /**
     * spec 1636 / T2423：校准系数建议（可操作化读数）——持续偏差时给出把估算
     * 拉回真值的乘法系数：suggested = mean(actual/estimated)（正偏差=高估→
     * 系数 &lt;1 调低）。样本不足（pairs < minSamples）或均值不可算 = empty
     * （诚実：不基于噪声给建议）。
     */
    public static java.util.Optional<Double> calibrationFactorSuggestion(int minSamples) {
        EstimatorCalibrationAudit.Calibration c = calibration();
        if (c.pairs() < minSamples || c.meanRelativeError() == 0) {
            return java.util.Optional.empty();
        }
        // meanRelativeError = mean((est-act)/act) → act/est = 1/(1+e)（一阶近似）
        double suggested = 1.0 / (1.0 + c.meanRelativeError());
        if (!(suggested > 0) || Double.isInfinite(suggested)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Math.round(suggested * 10_000) / 10_000.0);
    }
}
