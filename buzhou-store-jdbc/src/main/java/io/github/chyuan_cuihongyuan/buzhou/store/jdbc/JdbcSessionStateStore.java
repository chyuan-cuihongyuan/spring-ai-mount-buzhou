package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class JdbcSessionStateStore implements SessionStateStore {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StateEntry> MAPPER = (rs, n) -> new StateEntry(
            rs.getString("state_key"),
            rs.getString("state_value"),
            rs.getString("producer"),
            rs.getInt("created_turn"),
            rs.getObject("ttl_turns", Integer.class),
            rs.getTimestamp("updated_at").toInstant());

    public JdbcSessionStateStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void put(String sessionId, StateEntry entry) {
        delete(sessionId, entry.key());
        jdbc.update("""
                        INSERT INTO buzhou_session_state
                        (session_id, state_key, state_value, producer, created_turn, ttl_turns, updated_at)
                        VALUES (?,?,?,?,?,?,?)
                        """,
                sessionId, entry.key(), entry.value(), entry.producer(),
                entry.createdTurn(), entry.ttlTurns(), Timestamp.from(entry.updatedAt()));
    }

    @Override
    public Optional<StateEntry> get(String sessionId, String key) {
        return jdbc.query("""
                        SELECT * FROM buzhou_session_state WHERE session_id = ? AND state_key = ?
                        """, MAPPER, sessionId, key).stream().findFirst();
    }

    @Override
    public Map<String, StateEntry> getAll(String sessionId) {
        Map<String, StateEntry> result = new LinkedHashMap<>();
        jdbc.query("SELECT * FROM buzhou_session_state WHERE session_id = ?", MAPPER, sessionId)
                .forEach(entry -> result.put(entry.key(), entry));
        return result;
    }

    @Override
    public void delete(String sessionId, String key) {
        jdbc.update("DELETE FROM buzhou_session_state WHERE session_id = ? AND state_key = ?",
                sessionId, key);
    }

    @Override
    public boolean deleteIfValueMatches(String sessionId, String key, String expectedValue) {
        // 带 value 条件的 DELETE：影响行数 1 = CAS 删除成功（HITL 一次性授权原子消费）
        return jdbc.update("""
                        DELETE FROM buzhou_session_state
                        WHERE session_id = ? AND state_key = ? AND state_value = ?
                        """, sessionId, key, expectedValue) == 1;
    }

    @Override
    public boolean putIfAbsent(String sessionId, StateEntry entry) {
        // 原子 put-if-absent（幂等去重 reserve）：主键 (session_id, state_key) 冲突即放弃，
        // 影响行数 1 = 占位成功。捕获主键冲突异常而非先查后插，保证并发下原子语义。
        try {
            return jdbc.update("""
                            INSERT INTO buzhou_session_state
                            (session_id, state_key, state_value, producer, created_turn, ttl_turns, updated_at)
                            VALUES (?,?,?,?,?,?,?)
                            """,
                    sessionId, entry.key(), entry.value(), entry.producer(),
                    entry.createdTurn(), entry.ttlTurns(), Timestamp.from(entry.updatedAt())) == 1;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return false;
        }
    }
}
