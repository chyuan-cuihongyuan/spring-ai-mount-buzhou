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

    /** impl-35 / spec 13 §stores-6：单表批量删（幂等；单语句自原子）。 */
    @Override
    public void deleteSession(String sessionId) {
        jdbc.update("DELETE FROM buzhou_session_state WHERE session_id = ?", sessionId);
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

    /** spec 56 §A / T249：CAS 条件写——条件单语句影响行数判定（免方言 MERGE，H2/MySQL/PG 可移植）。 */
    @Override
    public boolean compareAndSwap(String sessionId, String key, String expectedValue, StateEntry update) {
        if (expectedValue != null) {
            return jdbc.update("""
                            UPDATE buzhou_session_state
                            SET state_value = ?, producer = ?, created_turn = ?, ttl_turns = ?, updated_at = ?
                            WHERE session_id = ? AND state_key = ? AND state_value = ?
                            """,
                    update.value(), update.producer(), update.createdTurn(), update.ttlTurns(),
                    Timestamp.from(update.updatedAt()), sessionId, key, expectedValue) == 1;
        }
        // expected=null：键不存在才插（NOT EXISTS 单语句——并发双插只有一方影响行数为 1）
        return jdbc.update("""
                        INSERT INTO buzhou_session_state
                        (session_id, state_key, state_value, producer, created_turn, ttl_turns, updated_at)
                        SELECT ?,?,?,?,?,?,? WHERE NOT EXISTS
                        (SELECT 1 FROM buzhou_session_state WHERE session_id = ? AND state_key = ?)
                        """,
                sessionId, update.key(), update.value(), update.producer(), update.createdTurn(),
                update.ttlTurns(), Timestamp.from(update.updatedAt()), sessionId, key) == 1;
    }

    /** spec 33 §C / T114：前缀下推扫描（键条件走索引；prefix 为内部常量无 LIKE 元字符）。 */
    @Override
    public Map<String, StateEntry> scanByPrefix(String sessionId, String prefix) {
        Map<String, StateEntry> result = new LinkedHashMap<>();
        jdbc.query("SELECT * FROM buzhou_session_state WHERE session_id = ? AND state_key LIKE ?",
                MAPPER, sessionId, prefix + "%")
                .forEach(entry -> result.put(entry.key(), entry));
        return result;
    }

    /** spec 58 §A / T259：COUNT 下推（容量检查零行传输）。 */
    @Override
    public int countByPrefix(String sessionId, String prefix) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM buzhou_session_state WHERE session_id = ? AND state_key LIKE ?",
                Integer.class, sessionId, prefix + "%");
        return count == null ? 0 : count;
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
