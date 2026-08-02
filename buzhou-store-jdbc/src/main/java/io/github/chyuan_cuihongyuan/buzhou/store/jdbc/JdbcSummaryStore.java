package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SummaryStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcSummaryStore implements SummaryStore {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StructuredSummary> MAPPER = (rs, n) -> new StructuredSummary(
            rs.getString("session_id"),
            rs.getLong("version"),
            JdbcJson.readMap(rs.getString("sections")).entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey, e -> String.valueOf(e.getValue()))),
            rs.getInt("token_estimate"),
            rs.getTimestamp("created_at").toInstant());

    public JdbcSummaryStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long save(String sessionId, StructuredSummary summary) {
        Long next = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version), 0) + 1 FROM buzhou_summary WHERE session_id = ?",
                Long.class, sessionId);
        jdbc.update("""
                        INSERT INTO buzhou_summary (session_id, version, sections, token_estimate, created_at)
                        VALUES (?,?,?,?,?)
                        """,
                sessionId, next, JdbcJson.write(summary.sections()),
                summary.tokenEstimate(), Timestamp.from(summary.createdAt()));
        return next;
    }

    @Override
    public Optional<StructuredSummary> latest(String sessionId) {
        return jdbc.query("""
                        SELECT * FROM buzhou_summary WHERE session_id = ?
                        ORDER BY version DESC LIMIT 1
                        """, MAPPER, sessionId).stream().findFirst();
    }

    @Override
    public List<StructuredSummary> history(String sessionId, int limit) {
        return jdbc.query("""
                        SELECT * FROM buzhou_summary WHERE session_id = ?
                        ORDER BY version DESC LIMIT ?
                        """, MAPPER, sessionId, limit);
    }
}
