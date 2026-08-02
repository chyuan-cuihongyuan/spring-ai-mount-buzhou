package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class JdbcObservabilityStore implements ObservabilityStore {

    private final JdbcTemplate jdbc;

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
        this.jdbc = jdbc;
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
}
