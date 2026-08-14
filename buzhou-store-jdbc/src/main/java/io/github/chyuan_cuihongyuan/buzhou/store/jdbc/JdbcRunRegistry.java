package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStateSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Run 注册表 JDBC 实现（wayfinder2 impl-06；契约与 InMemoryRunRegistry 一致）。 */
public class JdbcRunRegistry implements RunRegistry {

    private static final RowMapper<RunStateSnapshot> MAPPER = (rs, n) -> new RunStateSnapshot(
            rs.getString("session_id"),
            rs.getString("app_id"),
            rs.getString("agent_name"),
            RunStatus.valueOf(rs.getString("status")),
            rs.getInt("current_turn"),
            rs.getInt("last_completed_turn"),
            rs.getString("owner_id"),
            rs.getTimestamp("updated_at").toInstant());

    private final JdbcTemplate jdbc;

    /** 共享事务模板（spec 13 §stores-7 / ticket 32）：null = 兼容旧自动提交路径。 */
    @Nullable
    private final TransactionTemplate transactionTemplate;

    public JdbcRunRegistry(JdbcTemplate jdbc) {
        this(jdbc, null);
    }

    public JdbcRunRegistry(JdbcTemplate jdbc, @Nullable TransactionTemplate transactionTemplate) {
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void save(RunStateSnapshot snapshot) {
        // ticket 32：方言无关 upsert（先删后插）整段进同一事务（有 UoW 复用、无则自开短事务），
        // 与 JdbcSessionStateStore.put 同惯例
        JdbcTransactions.inCurrentOrNew(transactionTemplate, () -> {
            jdbc.update("DELETE FROM buzhou_run_registry WHERE session_id = ?", snapshot.sessionId());
            jdbc.update("""
                            INSERT INTO buzhou_run_registry
                            (session_id, app_id, agent_name, status, current_turn,
                             last_completed_turn, owner_id, updated_at)
                            VALUES (?,?,?,?,?,?,?,?)
                            """,
                    snapshot.sessionId(), snapshot.appId(), snapshot.agentName(),
                    snapshot.status().name(), snapshot.currentTurn(), snapshot.lastCompletedTurn(),
                    snapshot.ownerId(), Timestamp.from(snapshot.updatedAt()));
            return null;
        });
    }

    @Override
    public Optional<RunStateSnapshot> find(String sessionId) {
        return jdbc.query("SELECT * FROM buzhou_run_registry WHERE session_id = ?",
                MAPPER, sessionId).stream().findFirst();
    }

    @Override
    public List<RunStateSnapshot> list(RunStatus status) {
        return jdbc.query("SELECT * FROM buzhou_run_registry WHERE status = ?",
                        MAPPER, status.name()).stream()
                .sorted(Comparator.comparing(RunStateSnapshot::updatedAt))
                .toList();
    }

    /** impl-35 / spec 13 §stores-6：单表批量删（幂等；单语句自原子）。 */
    @Override
    public void deleteSession(String sessionId) {
        jdbc.update("DELETE FROM buzhou_run_registry WHERE session_id = ?", sessionId);
    }
}
