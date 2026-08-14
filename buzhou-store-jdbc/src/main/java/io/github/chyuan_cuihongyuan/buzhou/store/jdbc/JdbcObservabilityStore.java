package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcObservabilityStore implements ObservabilityStore {

    private final JdbcTemplate jdbc;

    /** 共享事务模板（impl-35 / spec 13 §stores-6：三表级联删同事务）：null = 兼容旧自动提交路径。 */
    @Nullable
    private final TransactionTemplate transactionTemplate;

    private static final RowMapper<SpanRecord> SPAN_MAPPER = (rs, n) -> new SpanRecord(
            rs.getString("span_id"),
            rs.getString("parent_id"),
            rs.getString("session_id"),
            rs.getInt("turn_seq"),
            rs.getString("kind"),
            rs.getString("name"),
            rs.getTimestamp("started_at").toInstant(),
            rs.getTimestamp("ended_at") == null ? null : rs.getTimestamp("ended_at").toInstant(),
            rs.getString("status"),
            JdbcJson.readMap(rs.getString("attributes")));

    private static final RowMapper<EventRecord> EVENT_MAPPER = (rs, n) -> new EventRecord(
            rs.getString("event_id"),
            rs.getString("span_id"),
            rs.getString("session_id"),
            rs.getString("kind"),
            rs.getTimestamp("created_at").toInstant(),
            JdbcJson.readMap(rs.getString("payload")));

    public JdbcObservabilityStore(JdbcTemplate jdbc) {
        this(jdbc, null);
    }

    public JdbcObservabilityStore(JdbcTemplate jdbc, @Nullable TransactionTemplate transactionTemplate) {
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * impl-35 / spec 13 §stores-6：三表（span / event / injection_snapshot）级联删
     * 进同一事务（有 UoW 复用、无则自开短事务）——崩溃窗口不留下「删一半」的观测残留。幂等。
     */
    @Override
    public void deleteSession(String sessionId) {
        JdbcTransactions.inCurrentOrNew(transactionTemplate, () -> {
            jdbc.update("DELETE FROM buzhou_span WHERE session_id = ?", sessionId);
            jdbc.update("DELETE FROM buzhou_event WHERE session_id = ?", sessionId);
            jdbc.update("DELETE FROM buzhou_injection_snapshot WHERE session_id = ?", sessionId);
            return null;
        });
    }

    @Override
    public void saveSpans(List<SpanRecord> spans) {
        jdbc.batchUpdate("""
                        INSERT INTO buzhou_span
                        (span_id, session_id, turn_seq, parent_id, kind, name,
                         started_at, ended_at, status, attributes)
                        VALUES (?,?,?,?,?,?,?,?,?,?)
                        """,
                spans, spans.size(), (ps, s) -> {
                    ps.setString(1, s.spanId());
                    ps.setString(2, s.sessionId());
                    ps.setInt(3, s.turnSeq());
                    ps.setString(4, s.parentSpanId());
                    ps.setString(5, s.kind());
                    ps.setString(6, s.name());
                    ps.setTimestamp(7, Timestamp.from(s.startedAt()));
                    ps.setTimestamp(8, s.endedAt() == null ? null : Timestamp.from(s.endedAt()));
                    ps.setString(9, s.status());
                    ps.setString(10, JdbcJson.write(s.attributes()));
                });
    }

    @Override
    public void saveEvents(List<EventRecord> events) {
        jdbc.batchUpdate("""
                        INSERT INTO buzhou_event (event_id, span_id, session_id, kind, payload, created_at)
                        VALUES (?,?,?,?,?,?)
                        """,
                events, events.size(), (ps, e) -> {
                    ps.setString(1, e.eventId());
                    ps.setString(2, e.spanId());
                    ps.setString(3, e.sessionId());
                    ps.setString(4, e.type());
                    ps.setString(5, JdbcJson.write(e.payload()));
                    ps.setTimestamp(6, Timestamp.from(e.occurredAt()));
                });
    }

    @Override
    public List<SpanRecord> spansOfSession(String sessionId) {
        return jdbc.query("SELECT * FROM buzhou_span WHERE session_id = ? ORDER BY started_at",
                SPAN_MAPPER, sessionId);
    }

    @Override
    public List<EventRecord> eventsOfSession(String sessionId) {
        return jdbc.query("SELECT * FROM buzhou_event WHERE session_id = ? ORDER BY created_at",
                EVENT_MAPPER, sessionId);
    }

    @Override
    public void saveInjectionSnapshot(InjectionSnapshot snapshot) {
        jdbc.update("""
                        INSERT INTO buzhou_injection_snapshot
                        (session_id, turn_seq, messages, budget_detail, created_at)
                        VALUES (?,?,?,?,?)
                        """,
                snapshot.sessionId(), snapshot.turnSeq(),
                JdbcJson.write(snapshot.messageIds()),
                JdbcJson.write(snapshot.budgetBreakdown()),
                Timestamp.from(snapshot.createdAt()));
    }

    @Override
    public Optional<InjectionSnapshot> injectionSnapshot(String sessionId, int turnSeq) {
        return jdbc.query("""
                        SELECT * FROM buzhou_injection_snapshot WHERE session_id = ? AND turn_seq = ?
                        """,
                (rs, n) -> new InjectionSnapshot(
                        rs.getString("session_id"),
                        rs.getInt("turn_seq"),
                        JdbcJson.readList(rs.getString("messages"), String.class),
                        JdbcJson.readMap(rs.getString("budget_detail")),
                        rs.getTimestamp("created_at").toInstant()),
                sessionId, turnSeq).stream().findFirst();
    }

    /** 会话汇总 SQL：TURN 计数经常量注入（SpanKind），COALESCE 与 SpanRecord.activityAt 同语义。 */
    private static final String SESSION_SUMMARY_SQL = """
            SELECT session_id,
                   MIN(started_at) AS first_at,
                   MAX(COALESCE(ended_at, started_at)) AS last_at,
                   SUM(CASE WHEN kind = '%s' THEN 1 ELSE 0 END) AS turn_count,
                   COUNT(*) AS span_count
            FROM buzhou_span
            GROUP BY session_id
            ORDER BY last_at DESC, session_id
            LIMIT ? OFFSET ?
            """.formatted(SpanKind.TURN);

    @Override
    public List<SessionSummary> listSessionSummaries(String cursor, int size) {
        int offset = cursor == null || cursor.isBlank() ? 0 : Integer.parseInt(cursor);
        List<SessionSummary> page = jdbc.query(SESSION_SUMMARY_SQL,
                (rs, n) -> new SessionSummary(
                        rs.getString("session_id"),
                        rs.getTimestamp("first_at").toInstant(),
                        rs.getTimestamp("last_at").toInstant(),
                        rs.getInt("turn_count"),
                        rs.getInt("span_count"),
                        Map.of()),
                size, offset);
        // SESSION span 属性袋按页内会话逐条补齐（页大小上限内，开发调试量级可接受——
        // spec 03 推演块第 3 条已声明 N+1 取舍）；ORDER BY 保证多 SESSION span 时取最早一条
        return page.stream().map(s -> jdbc.query("""
                        SELECT attributes FROM buzhou_span
                        WHERE session_id = ? AND kind = ? ORDER BY started_at LIMIT 1
                        """,
                        (rs, n) -> JdbcJson.readMap(rs.getString("attributes")),
                        s.sessionId(), SpanKind.SESSION)
                .stream().findFirst()
                .map(attrs -> new SessionSummary(s.sessionId(), s.firstActivityAt(),
                        s.lastActivityAt(), s.turnCount(), s.spanCount(), attrs))
                .orElse(s)).toList();
    }

    @Override
    public List<EventRecord> eventsOfSpan(String spanId) {
        return jdbc.query("SELECT * FROM buzhou_event WHERE span_id = ? ORDER BY created_at",
                EVENT_MAPPER, spanId);
    }
}
