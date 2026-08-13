package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 事件溯源工具调用日志内存实现（JDBC 实现见 store-jdbc 模块，契约一致）。 */
public class InMemoryToolCallLog implements ToolCallLog {

    private final Map<String, ToolCallLogEntry> entries = new ConcurrentHashMap<>();

    @Override
    public void append(ToolCallLogEntry entry) {
        entries.merge(key(entry.sessionId(), entry.toolCallId()), entry, (existing, incoming) ->
                existing.outcome() == ToolCallOutcome.COMPLETED ? existing : incoming);
    }

    @Override
    public Optional<ToolCallLogEntry> find(String sessionId, String toolCallId) {
        return Optional.ofNullable(entries.get(key(sessionId, toolCallId)));
    }

    private static String key(String sessionId, String toolCallId) {
        return sessionId + ":" + toolCallId;
    }
}
