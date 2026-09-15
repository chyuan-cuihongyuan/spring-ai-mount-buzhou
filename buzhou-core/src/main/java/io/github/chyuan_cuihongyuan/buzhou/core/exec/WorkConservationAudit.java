package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.List;

/**
 * 保工作性审计（spec 1857 / T2915 / impl 1458）——调度理论 work
 * conservation（保工作性）思想：调度器**不让容量闲置在有活可干时**——
 * 任何队列有积压而任何已分配容量在空转，即保工作性违例（容量白闲）。
 * 公平调度算法（WFQ/DRR）的核心性质：不公平可以谈（权重策略），不保
 * 工作不可恕（纯浪费）。审计读数：违例轮数、违例时闲置容量对积压的
 * 覆盖比（浪费面有多大）。
 *
 * <p>纯函数零状态、只审计不调度（调度归宿主）。
 */
public final class WorkConservationAudit {

    private WorkConservationAudit() {
    }

    /** 单时隙快照契约：queue 非空白、backlog ≥ 0、allocatedCapacity ≥ 0。 */
    public record Slot(String queue, long backlog, long allocatedCapacity) {

        public Slot {
            if (queue == null || queue.isBlank() || backlog < 0
                    || allocatedCapacity < 0) {
                throw new IllegalArgumentException(String.format(
                        "非法时隙：queue=%s, backlog=%d, capacity=%d",
                        queue, backlog, allocatedCapacity));
            }
        }
    }

    /**
     * @param slots           观测时隙总数
     * @param violations      违例时隙数（该时隙内有队列积压且有容量闲置）
     * @param totalIdleWithBacklog 违例时隙内闲置容量合计（浪费面）
     * @param totalBacklogDuringViolation 违例时隙内积压合计（对照面）
     */
    public record Report(int slots, long violations, long totalIdleWithBacklog,
                         long totalBacklogDuringViolation) {

        /** 违例率（无时隙 -1 哨兵）。 */
        public double violationRatio() {
            return slots == 0 ? -1d : (double) violations / slots;
        }

        /** 违例时浪费覆盖比 = 闲置容量/同期积压（≥1 即闲置足以清空积压
         *——纯浪费；无违例 -1 哨兵）。 */
        public double wasteCoverageRatio() {
            return violations == 0 ? -1d
                    : (double) totalIdleWithBacklog / totalBacklogDuringViolation;
        }
    }

    /**
     * 审计入口：逐时隙判违例（同一时隙快照内：存在 backlog>0 的队列 且
     * 存在 allocatedCapacity>0 但 backlog==0 的队列——容量闲着而别处有活）。
     * null 按空表。
     */
    public static Report audit(List<List<Slot>> slotsPerInstant) {
        List<List<Slot>> window =
                slotsPerInstant == null ? List.of() : slotsPerInstant;
        long violations = 0;
        long idleSum = 0;
        long backlogSum = 0;
        for (List<Slot> instant : window) {
            List<Slot> slots = instant == null ? List.of() : instant;
            boolean anyBacklog = false;
            long idleCapacity = 0;
            long backlogTotal = 0;
            for (Slot s : slots) {
                if (s.backlog() > 0) {
                    anyBacklog = true;
                    backlogTotal += s.backlog();
                } else if (s.allocatedCapacity() > 0) {
                    idleCapacity += s.allocatedCapacity();
                }
            }
            if (anyBacklog && idleCapacity > 0) {
                violations++;
                idleSum += idleCapacity;
                backlogSum += backlogTotal;
            }
        }
        return new Report(window.size(), violations, idleSum, backlogSum);
    }
}
