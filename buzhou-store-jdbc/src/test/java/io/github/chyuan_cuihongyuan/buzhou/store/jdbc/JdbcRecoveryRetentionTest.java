package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStateSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-37 / spec 13 §stores-6 + §growth-8：恢复设施窗口批删的 H2 验证——
 * ToolCallLog 保留窗口外删除；RunRegistry 仅清 COMPLETED（RUNNING 供恢复巡检）。
 */
class JdbcRecoveryRetentionTest {

    private final JdbcToolCallLog toolCallLog;
    private final JdbcRunRegistry runRegistry;

    JdbcRecoveryRetentionTest() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:recovery-retention-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        SchemaMigrator.migrate(dataSource, Dialect.H2);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        toolCallLog = new JdbcToolCallLog(jdbc);
        runRegistry = new JdbcRunRegistry(jdbc);
    }

    @Test
    void toolCallLogPruneRemovesOnlyOutsideWindow() {
        Instant old = Instant.now().minus(Duration.ofDays(8));
        toolCallLog.append(new ToolCallLogEntry("ret-1", "call-old", "echo", "h",
                ToolCallOutcome.COMPLETED, "ok", old));
        toolCallLog.append(new ToolCallLogEntry("ret-1", "call-new", "echo", "h",
                ToolCallOutcome.COMPLETED, "ok", Instant.now()));

        int pruned = toolCallLog.prune(Instant.now().minus(Duration.ofDays(7)));

        assertThat(pruned).isEqualTo(1);
        assertThat(toolCallLog.find("ret-1", "call-old")).isEqualTo(Optional.empty());
        assertThat(toolCallLog.find("ret-1", "call-new")).isPresent();
    }

    @Test
    void runRegistryPruneRemovesOnlyCompletedOutsideWindow() {
        Instant old = Instant.now().minus(Duration.ofDays(2));
        runRegistry.save(new RunStateSnapshot("ret-done", "app", "agent", RunStatus.COMPLETED,
                1, 1, "owner", old));
        runRegistry.save(new RunStateSnapshot("ret-live", "app", "agent", RunStatus.RUNNING,
                1, 0, "owner", old));

        int pruned = runRegistry.pruneCompletedBefore(Instant.now().minus(Duration.ofHours(24)));

        assertThat(pruned).isEqualTo(1);
        assertThat(runRegistry.find("ret-done")).isEqualTo(Optional.empty());
        assertThat(runRegistry.find("ret-live")).isPresent();
    }
}
