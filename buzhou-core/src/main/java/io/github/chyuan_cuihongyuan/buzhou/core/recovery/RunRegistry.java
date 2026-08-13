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
}
