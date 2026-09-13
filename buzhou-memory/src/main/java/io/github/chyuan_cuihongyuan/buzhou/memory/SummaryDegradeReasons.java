package io.github.chyuan_cuihongyuan.buzhou.memory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 摘要降级原因分布（spec 844 / T1189，Envoy degraded 健康语义扩散；
 * {@code SummaryDegrader} 降级管线的统计面）：降级事件按原因五态闭集
 * （超限截断/生成失败/内容为空/策略强制/未知）计数+占比+每段关联计数
 * ——「摘要为什么在降级、降的是谁」从管线日志变分布表。
 *
 * <p>纯记账：原因五态闭集天然有界；synchronized 计数；snapshot 占比降序
 * 平局声明序；null 忽略；空真。喂点=降级管线装配侧（不改 degrader 行为）。
 */
public final class SummaryDegradeReasons {

    /** 降级原因五态。 */
    public enum Reason {
        /** 超 token 上限截断。 */
        OVER_LIMIT,
        /** 生成调用失败（降级到旧版/占位）。 */
        GENERATION_FAILED,
        /** 内容为空无从降级。 */
        EMPTY_CONTENT,
        /** 策略/配置强制降级。 */
        POLICY_FORCED,
        /** 未归类（诚实兜底）。 */
        UNKNOWN
    }

    /** 单原因行。 */
    public record ReasonCount(Reason reason, long count, double share) {
    }

    /** 不可变报告（占比降序）。 */
    public record Report(List<ReasonCount> reasons, long total) {
    }

    private final Map<Reason, Long> counters = new EnumMap<>(Reason.class);
    private long total;

    /** 记录一次降级（null 忽略）。 */
    public synchronized void record(Reason reason) {
        if (reason == null) {
            return;
        }
        counters.merge(reason, 1L, Long::sum);
        total++;
    }

    /** 只读快照（占比降序，平局声明序）。 */
    public synchronized Report snapshot() {
        List<ReasonCount> rows = new java.util.ArrayList<>(counters.size());
        for (Map.Entry<Reason, Long> e : counters.entrySet()) {
            rows.add(new ReasonCount(e.getKey(), e.getValue(),
                    total == 0 ? 0 : (double) e.getValue() / total));
        }
        rows.sort(java.util.Comparator.comparingLong(ReasonCount::count).reversed());
        return new Report(List.copyOf(rows), total);
    }
}
