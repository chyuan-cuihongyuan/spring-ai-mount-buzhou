package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.Optional;

/**
 * 事件溯源工具调用日志（wayfinder2 impl-07 / T33 / docs/spec/12）：
 * <b>不引入 workflow engine</b>（Temporal/Dapr 思想、非引擎），只取两点——
 * ① append-only 记录；② 恢复时按 (sessionId, toolCallId) 查找已落盘结局，
 * 命中 COMPLETED 则短路不重跑（DanglingCallRepairer 回放，exactly-once）。
 * 恢复点 = 最后 Completed-Turn 之后，天然不重放 LLM。
 */
public interface ToolCallLog {

    /**
     * 追加条目（append-only）：同键已有 <b>COMPLETED</b> 条目时忽略后续追加
     * （已完成的事实只记录一次——Temporal Activity 结果语义）。
     */
    void append(ToolCallLogEntry entry);

    Optional<ToolCallLogEntry> find(String sessionId, String toolCallId);
}
