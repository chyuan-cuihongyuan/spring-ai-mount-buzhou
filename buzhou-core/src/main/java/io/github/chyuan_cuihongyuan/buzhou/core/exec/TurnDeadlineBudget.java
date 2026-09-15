package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.ArrayList;
import java.util.List;

/**
 * 轮墙钟预算的传播裁决（spec 1802 / T2805 / impl 1403）——gRPC deadline
 * propagation / Temporal schedule-to-close 思想：预算沿顺序调用链**传播并
 * 递减**——每次派发前检查剩余，超支调用拿到截断超时（min 规则），预算耗尽
 * 后的调用直接拒绝（DEADLINE_EXCEEDED 语义——不起工），而不是各自独立
 * 重新计时把一轮拖成 N 倍墙钟。
 *
 * <p>纯函数零状态：输入为轮总预算与各调用预估耗时（预估口径由调用方声明），
 * 输出逐调用裁决。只裁决不执行（派发归宿主）。
 */
public final class TurnDeadlineBudget {

    private TurnDeadlineBudget() {
    }

    /** 单调用裁决：admitted=false 即 DEADLINE_EXCEEDED（不派发）；否则带截断超时。 */
    public record CallAllocation(int callIndex, boolean admitted, long effectiveTimeoutNanos) {
    }

    /**
     * @param totalNanos     轮总墙钟预算（≥0）
     * @param allocations    逐调用裁决（入参序）
     * @param committedNanos 已获准调用占用的预算合计（截断后口径）
     */
    public record BudgetPlan(long totalNanos, List<CallAllocation> allocations,
                             long committedNanos) {

        /** 获准率 = admitted/allocations（空计划 -1 哨兵）。 */
        public double admissionRatio() {
            return allocations.isEmpty() ? -1d
                    : (double) allocations.stream().filter(CallAllocation::admitted).count()
                            / allocations.size();
        }
    }

    /**
     * 传播裁决入口。契约：totalNanos ≥ 0、每项预估 ≥ 0（fail-fast）；null 按空表。
     * 语义：剩余预算顺序递减；剩余 = 0 → 拒绝（零耗预估也不例外——deadline
     * 先于派发检查）；获准调用超时 = min(预估, 剩余)。
     */
    public static BudgetPlan plan(long totalNanos, List<Long> estimatedNanos) {
        if (totalNanos < 0) {
            throw new IllegalArgumentException("totalNanos 不能为负：" + totalNanos);
        }
        List<Long> estimates = estimatedNanos == null ? List.of() : estimatedNanos;
        List<CallAllocation> allocations = new ArrayList<>(estimates.size());
        long remaining = totalNanos;
        long committed = 0;
        for (int i = 0; i < estimates.size(); i++) {
            long estimate = estimates.get(i);
            if (estimate < 0) {
                throw new IllegalArgumentException("第 " + i + " 项预估不能为负：" + estimate);
            }
            if (remaining == 0) {
                allocations.add(new CallAllocation(i, false, 0));
                continue;
            }
            long effective = Math.min(estimate, remaining);
            allocations.add(new CallAllocation(i, true, effective));
            committed += effective;
            remaining -= effective;
        }
        return new BudgetPlan(totalNanos, List.copyOf(allocations), committed);
    }
}
