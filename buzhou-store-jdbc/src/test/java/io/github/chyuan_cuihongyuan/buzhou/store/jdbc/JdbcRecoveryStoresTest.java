package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStateSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-06/07：JDBC Run 注册表 + 事件溯源工具日志的 H2 契约测试（与 InMemory 行为一致）。
 * 建库经 {@link SchemaMigrator} 版本化迁移（ticket 31 后取代 db/schema-h2.sql 直跑）。
 * ticket 32 增补：恢复设施多写的事务接线契约（UoW 内原子提交 / 中途失败整体回滚）。
 */
class JdbcRecoveryStoresTest {

    private JdbcRunRegistry runRegistry;
    private JdbcToolCallLog toolCallLog;

    /** 事务接线版组合（共享 TransactionTemplate，与生产装配同形状）。 */
    private JdbcBuzhouRecoveryStores wired;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:recovery-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        SchemaMigrator.migrate(dataSource, Dialect.H2);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        runRegistry = new JdbcRunRegistry(jdbc);
        toolCallLog = new JdbcToolCallLog(jdbc);
        wired = JdbcBuzhouStores.createWithRecovery(dataSource, Dialect.H2);
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

    @Test
    void shouldRollbackAllRecoveryWrites_whenUnitOfWorkFailsMidway() {
        String sessionId = "uw-rollback-" + UUID.randomUUID();
        // 恶意 UoW：先落既有事实，再在多写中途失败——验证回滚覆盖 tool_call_log /
        // run_registry / session_state 全部写入（且既有事实不受殃及）
        wired.toolCallLog().append(new ToolCallLogEntry(sessionId, "tc-existing", "tool",
                "hash-0", ToolCallOutcome.COMPLETED, "既有事实", Instant.now()));
        assertThatThrownBy(() -> wired.unitOfWork().executeInTransaction(sessionId, () -> {
            wired.toolCallLog().append(new ToolCallLogEntry(sessionId, "tc-new", "tool",
                    "hash-1", ToolCallOutcome.COMPLETED, "事务内写入", Instant.now()));
            wired.runRegistry().save(new RunStateSnapshot(sessionId, "app", "agent",
                    RunStatus.RUNNING, 1, 0, "owner-1", Instant.now()));
            wired.sessionStateStore().put(sessionId,
                    new StateEntry("fact.x", "v", "hook", 1, null, Instant.now()));
            throw new IllegalStateException("simulated mid-transaction failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(wired.toolCallLog().find(sessionId, "tc-new")).isEmpty();
        assertThat(wired.runRegistry().find(sessionId)).isEmpty();
        assertThat(wired.sessionStateStore().getAll(sessionId)).isEmpty();
        // 回滚不殃及 UoW 之前已提交的既有事实
        assertThat(wired.toolCallLog().find(sessionId, "tc-existing")).isPresent();
    }

    @Test
    void shouldCommitAllRecoveryWrites_whenUnitOfWorkSucceeds() {
        String sessionId = "uw-commit-" + UUID.randomUUID();
        wired.unitOfWork().executeInTransaction(sessionId, () -> {
            wired.toolCallLog().append(new ToolCallLogEntry(sessionId, "tc-ok", "tool",
                    "hash-1", ToolCallOutcome.COMPLETED, "事务内写入", Instant.now()));
            wired.runRegistry().save(new RunStateSnapshot(sessionId, "app", "agent",
                    RunStatus.RUNNING, 1, 0, "owner-1", Instant.now()));
            wired.sessionStateStore().put(sessionId,
                    new StateEntry("fact.x", "v", "hook", 1, null, Instant.now()));
            return null;
        });

        assertThat(wired.toolCallLog().find(sessionId, "tc-ok")).isPresent();
        assertThat(wired.runRegistry().find(sessionId)).isPresent();
        assertThat(wired.sessionStateStore().getAll(sessionId)).hasSize(1);
    }
}
