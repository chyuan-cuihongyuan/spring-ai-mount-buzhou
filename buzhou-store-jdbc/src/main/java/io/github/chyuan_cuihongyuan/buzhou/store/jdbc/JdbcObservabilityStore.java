package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.retention.ObservabilityTtl;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ClosedSession;
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
import java.time.Instant;
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

    /**
     * impl-37 / spec 13 §stores-6：封闭会话枚举——SESSION span 已结束且早于上界，
     * 按封闭时刻升序（LIMIT 批量限量同源）。活动会话（ended_at IS NULL）永不出现在结果。
     */
    @Override
    public List<ClosedSession> listClosedSessions(Instant closedBefore, int limit) {
        return jdbc.query("""
                        SELECT session_id, MAX(ended_at) AS closed_at
                        FROM buzhou_span
                        WHERE kind = ? AND ended_at IS NOT NULL AND ended_at < ?
                        GROUP BY session_id
                        ORDER BY closed_at
                        LIMIT ?
                        """,
                (rs, n) -> new ClosedSession(
                        rs.getString("session_id"),
                        rs.getTimestamp("closed_at").toInstant()),
                SpanKind.SESSION, Timestamp.from(closedBefore), limit);
    }

    /**
     * impl-37 / spec 13 §stores-6：观测 TTL 批删——events 按创建时间、spans 按最后活动
     * （COALESCE(ended_at, started_at)）过期，以派生表 LIMIT 子查询批删（MySQL 1093 规避：
     * 派生表包裹后可删本表）；同事务提交。
     */
    @Override
    public int prune(ObservabilityTtl policy) {
        Instant cutoff = Instant.now().minus(policy.ttl());
        return JdbcTransactions.inCurrentOrNew(transactionTemplate, () -> {
            int events = jdbc.update("""
                            DELETE FROM buzhou_event WHERE event_id IN
                            (SELECT event_id FROM (SELECT event_id FROM buzhou_event
                              WHERE created_at < ? LIMIT ?) AS batch)
                            """,
                    Timestamp.from(cutoff), policy.batchSize());
            int spans = jdbc.update("""
                            DELETE FROM buzhou_span WHERE span_id IN
                            (SELECT span_id FROM (SELECT span_id FROM buzhou_span
                              WHERE COALESCE(ended_at, started_at) < ? LIMIT ?) AS batch)
                            """,
                    Timestamp.from(cutoff), policy.batchSize());
            // 快照表低量（每 Turn 一行）无独立单列主键——直删不带 LIMIT（批删限量面向
            // 高流水 event/span；行值 IN 派生表对三方言兼容性得不偿失）
            int snapshots = jdbc.update(
                    "DELETE FROM buzhou_injection_snapshot WHERE created_at < ?",
                    Timestamp.from(cutoff));
            return events + spans + snapshots;
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
