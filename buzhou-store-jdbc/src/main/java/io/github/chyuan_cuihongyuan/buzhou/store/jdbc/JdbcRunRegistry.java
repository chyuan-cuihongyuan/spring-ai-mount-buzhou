package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStateSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

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

    public JdbcRunRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(RunStateSnapshot snapshot) {
        // 方言无关 upsert：先删后插（与 JdbcSessionStateStore.put 同惯例）
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
}
