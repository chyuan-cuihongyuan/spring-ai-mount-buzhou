package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.Optional;

/** 事件溯源工具调用日志 JDBC 实现（wayfinder2 impl-07；append-only、COMPLETED 只记录一次）。 */
public class JdbcToolCallLog implements ToolCallLog {

    private static final RowMapper<ToolCallLogEntry> MAPPER = (rs, n) -> new ToolCallLogEntry(
            rs.getString("session_id"),
            rs.getString("tool_call_id"),
            rs.getString("tool_name"),
            rs.getString("args_hash"),
            ToolCallOutcome.valueOf(rs.getString("outcome")),
            rs.getString("result"),
            rs.getTimestamp("occurred_at").toInstant());

    private final JdbcTemplate jdbc;

    public JdbcToolCallLog(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(ToolCallLogEntry entry) {
        // append-only：同键已有 COMPLETED 条目则忽略（事实只记录一次）；方言无关 upsert=先删后插
        Optional<ToolCallLogEntry> existing = find(entry.sessionId(), entry.toolCallId());
        if (existing.isPresent() && existing.get().outcome() == ToolCallOutcome.COMPLETED
                && entry.outcome() != ToolCallOutcome.COMPLETED) {
            return;
        }
        jdbc.update("DELETE FROM buzhou_tool_call_log WHERE session_id = ? AND tool_call_id = ?",
                entry.sessionId(), entry.toolCallId());
        jdbc.update("""
                        INSERT INTO buzhou_tool_call_log
                        (session_id, tool_call_id, tool_name, args_hash, outcome, result, occurred_at)
                        VALUES (?,?,?,?,?,?,?)
                        """,
                entry.sessionId(), entry.toolCallId(), entry.toolName(), entry.argsHash(),
                entry.outcome().name(), entry.result(), Timestamp.from(entry.occurredAt()));
    }

    @Override
    public Optional<ToolCallLogEntry> find(String sessionId, String toolCallId) {
        return jdbc.query(
                "SELECT * FROM buzhou_tool_call_log WHERE session_id = ? AND tool_call_id = ?",
                MAPPER, sessionId, toolCallId).stream().findFirst();
    }
}
