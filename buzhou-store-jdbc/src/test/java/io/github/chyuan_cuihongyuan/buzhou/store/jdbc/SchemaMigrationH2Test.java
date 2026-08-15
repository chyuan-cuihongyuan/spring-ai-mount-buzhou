package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStateSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ticket 31 / spec 13 §stores-5：迁移机制的 H2 内嵌验证（无 Docker 依赖）——
 * 空库全量建 + 版本行记录、重复启动幂等、旧库基线判定升级、全量工厂恢复设施可用。
 */
class SchemaMigrationH2Test {

    private static JdbcDataSource newDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:migration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        return dataSource;
    }

    private static java.util.List<Integer> appliedVersions(JdbcTemplate jdbc) {
        return jdbc.queryForList(
                "SELECT version FROM buzhou_schema_version ORDER BY version", Integer.class);
    }

    @Test
    void shouldCreateFullSchemaAndRecordVersions_whenDatabaseEmpty() {
        JdbcDataSource dataSource = newDataSource();
        int version = SchemaMigrator.migrate(dataSource, Dialect.H2);

        assertThat(version).isEqualTo(3);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(appliedVersions(jdbc)).containsExactly(1, 2, 3);
        // 全量业务表就位（含 V2 演示列 + V3 会话索引表——空库由 V1 基线直接带上）
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(reasoning_signature) FROM buzhou_message", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM buzhou_tool_call_log", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM buzhou_run_registry", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM buzhou_session_index", Integer.class)).isZero();
    }

    @Test
    void shouldStayGreenAndNotDuplicateVersions_whenMigrateRunsTwice() {
        JdbcDataSource dataSource = newDataSource();
        SchemaMigrator.migrate(dataSource, Dialect.H2);

        // 第二次启动（幂等回归）：不抛异常、版本行不重复
        assertThatCode(() -> SchemaMigrator.migrate(dataSource, Dialect.H2))
                .doesNotThrowAnyException();
        assertThat(appliedVersions(new JdbcTemplate(dataSource))).containsExactly(1, 2, 3);
    }

    @Test
    void shouldBaselineWithoutRerunningV1AndApplyV2_whenLegacyTablesExistWithoutVersionRows()
            throws SQLException {
        JdbcDataSource dataSource = newDataSource();
        createLegacySchema(dataSource);

        int version = SchemaMigrator.migrate(dataSource, Dialect.H2);

        assertThat(version).isEqualTo(3);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(appliedVersions(jdbc)).containsExactly(1, 2, 3);
        // V1 行为基线采纳（不重跑建表），V2/V3 行为真实执行——旧数据保留 + 新列/新表生效
        assertThat(jdbc.queryForObject(
                "SELECT description FROM buzhou_schema_version WHERE version = 1", String.class))
                .contains("baseline");
        assertThat(jdbc.queryForObject(
                "SELECT reasoning_signature FROM buzhou_message WHERE id = 'legacy-1'", String.class))
                .isNull();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM buzhou_message WHERE session_id = 'legacy-session'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void shouldExposeWorkingRecoveryFacilities_whenCreatedWithRecoveryFactory() {
        JdbcBuzhouRecoveryStores full = JdbcBuzhouStores.createWithRecovery(newDataSource(), Dialect.H2);

        assertThat(full.messageStore()).isNotNull();
        full.runRegistry().save(new RunStateSnapshot(
                "mig-recovery", "app", "agent", RunStatus.RUNNING, 3, 2, "owner-1", null));
        assertThat(full.runRegistry().find("mig-recovery"))
                .hasValueSatisfying(snapshot -> assertThat(snapshot.lastCompletedTurn()).isEqualTo(2));

        full.toolCallLog().append(new ToolCallLogEntry(
                "mig-recovery", "tc-1", "write_db", "hash-1",
                ToolCallOutcome.COMPLETED, "已写入行 42", null));
        assertThat(full.toolCallLog().find("mig-recovery", "tc-1"))
                .hasValueSatisfying(entry -> assertThat(entry.outcome()).isEqualTo(ToolCallOutcome.COMPLETED));
    }

    /** 手工构造「迁移机制上线前」的旧库：业务表在（无 reasoning_signature 列）、版本表不在。 */
    private static void createLegacySchema(JdbcDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE buzhou_message (
                        id                VARCHAR(64)  PRIMARY KEY,
                        session_id        VARCHAR(128) NOT NULL,
                        turn_seq          INT          NOT NULL,
                        seq_in_turn       INT          NOT NULL,
                        role              VARCHAR(16)  NOT NULL,
                        content           CLOB,
                        tool_calls        CLOB,
                        tool_call_id      VARCHAR(64),
                        reasoning_content CLOB,
                        metadata          CLOB,
                        created_at        TIMESTAMP    NOT NULL
                    )""");
            statement.execute("""
                    INSERT INTO buzhou_message
                        (id, session_id, turn_seq, seq_in_turn, role, content, metadata, created_at)
                    VALUES
                        ('legacy-1', 'legacy-session', 1, 0, 'USER', '旧库存量消息', '{}', CURRENT_TIMESTAMP)""");
        }
    }
}
