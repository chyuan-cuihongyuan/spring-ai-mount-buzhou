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

    /** impl-35 / spec 13 §stores-6：移除该会话全部工具调用日志（复合键按前缀清除，幂等）。 */
    @Override
    public void deleteSession(String sessionId) {
        String prefix = sessionId + ":";
        entries.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /** impl-37 / spec 13 §stores-6：保留窗口批删（cutoff 之前发生的条目）。 */
    @Override
    public int prune(java.time.Instant cutoff) {
        return (int) entries.values().stream()
                .filter(e -> e.occurredAt().isBefore(cutoff))
                .map(e -> entries.remove(key(e.sessionId(), e.toolCallId())))
                .filter(java.util.Objects::nonNull)
                .count();
    }

    private static String key(String sessionId, String toolCallId) {
        return sessionId + ":" + toolCallId;
    }
}
