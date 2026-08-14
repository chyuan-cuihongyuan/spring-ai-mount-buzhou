package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.List;
import java.util.Optional;

/**
 * 持久 Run 注册表（wayfinder2 impl-06 / T32 / docs/spec/12）：把「悬空调用 reactive 修复」
 * 升级为 <b>proactive 恢复</b>——重启后枚举在途 run 并安全续跑。
 *
 * <p>来源 Mastra {@code WorkflowsStorage}（listWorkflowRuns/persistWorkflowSnapshot）——
 * Buzhou 以 Completed-Turn 为快照单元（比 step 粗、但与三级语义对齐），
 * 并规避其 restart 重跑已完结步骤的缺陷（续跑点恒为 lastCompletedTurn 之后）。
 */
public interface RunRegistry {

    /** upsert 快照。 */
    void save(RunStateSnapshot snapshot);

    Optional<RunStateSnapshot> find(String sessionId);

    /** 按状态枚举（恢复服务巡检 RUNNING 疑似崩溃者）。 */
    List<RunStateSnapshot> list(RunStatus status);

    /**
     * impl-35 / spec 13 §stores-6：删除该会话的 run 快照。幂等——
     * 会话不存在时无操作。默认 no-op（既有实现二进制兼容，由各实现补齐语义）。
     */
    default void deleteSession(String sessionId) {
    }

    /**
     * impl-37 / spec 13 §growth-8：COMPLETED 保留窗口批删（默认 PT24H）：删除
     * {@code cutoff} 之前完结的 COMPLETED 快照（RUNNING/INTERRUPTED 不受影响——
     * 恢复巡检依赖），返回删除条数。默认 no-op。
     */
    default int pruneCompletedBefore(java.time.Instant cutoff) {
        return 0;
    }
}
