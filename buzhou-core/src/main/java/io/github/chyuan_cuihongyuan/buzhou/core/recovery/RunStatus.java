package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

/**
 * Run 状态（wayfinder2 impl-06 / T32 / docs/spec/12，Mastra listWorkflowRuns 语义）：
 * 以会话为 run 单元、以 Completed-Turn 为快照边界。
 */
public enum RunStatus {
    /** 在途（含正常运行与疑似崩溃未恢复——配合租约判断）。 */
    RUNNING,
    /** 已被显式标记中断（如恢复服务接管前的旧快照）。 */
    INTERRUPTED,
    /** 会话已正常关闭。 */
    COMPLETED
}
