package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * HITL 认证决策分布（spec 842 / T1185，Keycloak required-actions 决策观测
 * 思想扩散）：HITL 审批五态（批准/拒绝/过期/已消费/未知凭证）计数+占比
 * ——「审批是被拒得多还是凭据管理出问题（过期/重复消费）」一屏分布。
 *
 * <p>纯记账（五态闭集天然有界）；synchronized 计数；snapshot 占比降序；
 * null 忽略。喂点=GuardAuthApi/确认 hook 装配侧。
 */
public final class AuthDecisionStats {

    /** 决策五态。 */
    public enum Outcome { GRANTED, DENIED, EXPIRED, CONSUMED, UNKNOWN }

    /** 单态行。 */
    public record OutcomeCount(Outcome outcome, long count, double share) {
    }

    /** 不可变报告（占比降序）。 */
    public record Report(List<OutcomeCount> outcomes, long total) {
    }

    private final Map<Outcome, Long> counters = new EnumMap<>(Outcome.class);
    private long total;

    /** 记录一次认证决策（null 忽略）。 */
    public synchronized void record(Outcome outcome) {
        if (outcome == null) {
            return;
        }
        counters.merge(outcome, 1L, Long::sum);
        total++;
    }

    /** 只读快照（占比降序，平局保持枚举声明序）。 */
    public synchronized Report snapshot() {
        List<OutcomeCount> rows = new java.util.ArrayList<>(counters.size());
        for (Map.Entry<Outcome, Long> e : counters.entrySet()) {
            rows.add(new OutcomeCount(e.getKey(), e.getValue(),
                    total == 0 ? 0 : (double) e.getValue() / total));
        }
        rows.sort(java.util.Comparator.comparingLong(OutcomeCount::count).reversed());
        return new Report(List.copyOf(rows), total);
    }
}
