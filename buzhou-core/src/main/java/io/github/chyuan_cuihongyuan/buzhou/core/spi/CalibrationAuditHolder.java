package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.Optional;

/**
 * Token 校准审计 Holder（spec 1618 / T2387，spec 819 孤类接线）：
 * TokenBudgetHook.afterModel 同点对账（CharHeuristic 估算 prompt vs 模型回报
 * usage.promptTokens）经此登记读出——「预算按估算设、账单按真实来」的系统性
 * 偏差从感觉变数字。
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
}
