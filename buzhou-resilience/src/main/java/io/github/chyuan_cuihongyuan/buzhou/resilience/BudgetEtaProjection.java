package io.github.chyuan_cuihongyuan.buzhou.resilience;

import java.util.List;

/**
 * 预算耗尽 ETA 投影（L 会话 1700 系 R47 = effort #1746 / spec 1746 /
 * 票 T2693 + T2694 / impl 1347）——Prometheus predict_linear / Google SRE
 * 预算烧尽预测思想：剩余预算与近期单位时间消耗速率 → 「还有多久烧完」
 * 的 ETA——告警在烧穿**之前**响，而不是烧穿之后。
 * {@link CostSpikeDetector} 管尖峰，本面管趋势外推。
 *
 * <p>纯函数零状态：`project(remainingBudget, recentSpendPerInterval,
 * intervalMillis)` → `EtaProjection(remaining/avgSpend/etaMillis/verdict)`。
 * avgSpend≤0（不花钱或无数据）→ etaMillis=−1（NEVER 哨兵，不会烧穿）。
 *
 * @since 1.0.0
 */
public final class BudgetEtaProjection {

    private BudgetEtaProjection() {
    }

    /** ETA 裁决闭集。 */
    public enum Verdict { NO_DATA, STABLE, PROJECTED }

    /**
     * @param remainingBudget   剩余预算（货币/令牌，同量纲）
     * @param avgSpendPerInterval 单位区间平均消耗
     * @param intervalMillis    区间毫秒
     * @param etaMillis         预计耗尽毫秒（≤0 消耗哨兵 −1 = 不会烧穿）
     * @param verdict           裁决（NO_DATA 无样本/STABLE 不花钱/PROJECTED 有投影）
     */
    public record EtaProjection(double remainingBudget, double avgSpendPerInterval,
                                long intervalMillis, long etaMillis, Verdict verdict) {
    }

    /** 投影入口：剩余预算 + 近期逐区间消耗序列。 */
    public static EtaProjection project(double remainingBudget,
                                        List<Double> recentSpendPerInterval,
                                        long intervalMillis) {
        List<Double> data = recentSpendPerInterval == null ? List.of() : recentSpendPerInterval;
        if (data.isEmpty()) {
            return new EtaProjection(remainingBudget, 0d, intervalMillis, -1L, Verdict.NO_DATA);
        }
        double avg = data.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
        if (avg <= 0d) {
            return new EtaProjection(remainingBudget, avg, intervalMillis, -1L, Verdict.STABLE);
        }
        long eta = (long) Math.floor(remainingBudget / avg * intervalMillis);
        return new EtaProjection(remainingBudget, avg, intervalMillis, eta, Verdict.PROJECTED);
    }
}
