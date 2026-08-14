package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class JdbcSessionStateStore implements SessionStateStore {

    private final JdbcTemplate jdbc;

    /** 共享事务模板（spec 13 §stores-7 / ticket 32）：null = 兼容旧自动提交路径。 */
    @Nullable
    private final TransactionTemplate transactionTemplate;

    private static final RowMapper<StateEntry> MAPPER = (rs, n) -> new StateEntry(
            rs.getString("state_key"),
            rs.getString("state_value"),
            rs.getString("producer"),
            rs.getInt("created_turn"),
            rs.getObject("ttl_turns", Integer.class),
            rs.getTimestamp("updated_at").toInstant());

    public JdbcSessionStateStore(JdbcTemplate jdbc) {
        this(jdbc, null);
    }

    public JdbcSessionStateStore(JdbcTemplate jdbc, @Nullable TransactionTemplate transactionTemplate) {
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void put(String sessionId, StateEntry entry) {
        // ticket 32：先删后插整段进同一事务（有 UoW 复用、无则自开短事务）
        JdbcTransactions.inCurrentOrNew(transactionTemplate, () -> {
            delete(sessionId, entry.key());
            jdbc.update("""
                            INSERT INTO buzhou_session_state
                            (session_id, state_key, state_value, producer, created_turn, ttl_turns, updated_at)
                            VALUES (?,?,?,?,?,?,?)
                            """,
                    sessionId, entry.key(), entry.value(), entry.producer(),
                    entry.createdTurn(), entry.ttlTurns(), Timestamp.from(entry.updatedAt()));
            return null;
        });
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
}
