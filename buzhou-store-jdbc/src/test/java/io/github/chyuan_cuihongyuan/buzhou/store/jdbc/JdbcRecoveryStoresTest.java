package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStateSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-06/07：JDBC Run 注册表 + 事件溯源工具日志的 H2 契约测试（与 InMemory 行为一致）。
 */
class JdbcRecoveryStoresTest {

    private JdbcRunRegistry runRegistry;
    private JdbcToolCallLog toolCallLog;

    @BeforeEach
    void setUp() throws java.sql.SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:recovery-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(
                dataSource.getConnection(),
                new org.springframework.core.io.ClassPathResource("db/schema-h2.sql"));
        runRegistry = new JdbcRunRegistry(jdbc);
        toolCallLog = new JdbcToolCallLog(jdbc);
    }

    @Test
    void runSnapshotUpsertAndListByStatus() {
        String sessionId = "run-" + UUID.randomUUID();
        runRegistry.save(new RunStateSnapshot(sessionId, "app", "agent",
                RunStatus.RUNNING, 3, 2, "owner-1", null));
        assertThat(runRegistry.find(sessionId)).hasValueSatisfying(snap -> {
            assertThat(snap.status()).isEqualTo(RunStatus.RUNNING);
            assertThat(snap.lastCompletedTurn()).isEqualTo(2);
        });
        // upsert 推进快照边界
        runRegistry.save(runRegistry.find(sessionId).orElseThrow().completingTurn(4, "owner-1"));
        assertThat(runRegistry.find(sessionId)).hasValueSatisfying(snap ->
                assertThat(snap.lastCompletedTurn()).isEqualTo(4));
        assertThat(runRegistry.list(RunStatus.RUNNING)).hasSize(1);
        runRegistry.save(runRegistry.find(sessionId).orElseThrow().withStatus(RunStatus.COMPLETED));
        assertThat(runRegistry.list(RunStatus.RUNNING)).isEmpty();
        assertThat(runRegistry.list(RunStatus.COMPLETED)).hasSize(1);
    }

    @Test
    void toolCallLogRecordsOnceWhenCompleted() {
        String sessionId = "log-" + UUID.randomUUID();
        String toolCallId = "tc-1";
        // FAILED → COMPLETED：结局可被覆盖（尚无成功事实）
        toolCallLog.append(new ToolCallLogEntry(sessionId, toolCallId, "write_db",
                "hash-1", ToolCallOutcome.FAILED, "执行失败：x", null));
        toolCallLog.append(new ToolCallLogEntry(sessionId, toolCallId, "write_db",
                "hash-1", ToolCallOutcome.COMPLETED, "已写入行 42", null));
        assertThat(toolCallLog.find(sessionId, toolCallId)).hasValueSatisfying(entry ->
                assertThat(entry.outcome()).isEqualTo(ToolCallOutcome.COMPLETED));
        // COMPLETED 后的追加不再改变记录（事实只记录一次——Temporal Activity 语义）
        toolCallLog.append(new ToolCallLogEntry(sessionId, toolCallId, "write_db",
                "hash-1", ToolCallOutcome.FAILED, "迟到的失败", null));
        assertThat(toolCallLog.find(sessionId, toolCallId)).hasValueSatisfying(entry -> {
            assertThat(entry.outcome()).isEqualTo(ToolCallOutcome.COMPLETED);
            assertThat(entry.result()).isEqualTo("已写入行 42");
        });
    }
}
