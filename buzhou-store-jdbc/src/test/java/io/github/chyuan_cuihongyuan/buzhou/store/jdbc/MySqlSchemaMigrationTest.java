package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ticket 31 / spec 13 §stores-5：MySQL（Testcontainers，无 Docker 自动跳过）——
 * 连续两次迁移幂等（老实现第二次启动因索引重名必失败的回归）、旧 schema 基线判定升级。
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MySqlSchemaMigrationTest {

    /** 旧 schema 基线升级用例的独立库名（与默认库隔离，共享同一容器）。 */
    private static final String LEGACY_DATABASE = "buzhou_legacy_upgrade";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    private static DataSource dataSource(String database) {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(com.mysql.cj.jdbc.Driver.class);
        dataSource.setUrl(rootUrl() + "/" + database);
        dataSource.setUsername(MYSQL.getUsername());
        dataSource.setPassword(MYSQL.getPassword());
        return dataSource;
    }

    /** 容器 JDBC 根地址（去掉默认库名，可指向任一 database）。 */
    private static String rootUrl() {
        String url = MYSQL.getJdbcUrl();
        return url.substring(0, url.lastIndexOf('/'));
    }

    private static List<Integer> appliedVersions(DataSource dataSource) {
        return new JdbcTemplate(dataSource).queryForList(
                "SELECT version FROM buzhou_schema_version ORDER BY version", Integer.class);
    }

    @Test
    void shouldStayGreenOnSecondStartup_whenMigratedTwiceConsecutively() {
        DataSource dataSource = dataSource(MYSQL.getDatabaseName());

        // 第一次冷启动：空库全量建
        assertThatCode(() -> SchemaMigrator.migrate(dataSource, Dialect.MYSQL))
                .doesNotThrowAnyException();
        // 第二次启动：幂等（老 schema-mysql.sql 的 CREATE INDEX 无 IF NOT EXISTS，此处必炸）
        assertThatCode(() -> SchemaMigrator.migrate(dataSource, Dialect.MYSQL))
                .doesNotThrowAnyException();

        assertThat(appliedVersions(dataSource)).containsExactly(1, 2);
        assertThat(new JdbcTemplate(dataSource).queryForObject(
                "SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS"
                        + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'buzhou_message'",
                Integer.class)).isEqualTo(2);
    }

    @Test
    void shouldBaselineLegacySchemaAndAddColumn_whenTablesExistWithoutVersionRows()
            throws SQLException {
        createLegacyDatabase();
        DataSource legacy = dataSource(LEGACY_DATABASE);

        int version = SchemaMigrator.migrate(legacy, Dialect.MYSQL);

        assertThat(version).isEqualTo(2);
        assertThat(appliedVersions(legacy)).containsExactly(1, 2);
        JdbcTemplate jdbc = new JdbcTemplate(legacy);
        // V1 行为基线采纳（不重跑建表脚本），V2 行为真实补列——旧数据保留
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

    /** 在容器内另建独立库并手工构造「迁移机制上线前」的旧 schema（无版本表、无 reasoning_signature 列）。 */
    private static void createLegacyDatabase() throws SQLException {
        try (Connection admin = DriverManager.getConnection(
                rootUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS " + LEGACY_DATABASE);
        }
        try (Connection connection = DriverManager.getConnection(
                rootUrl() + "/" + LEGACY_DATABASE, MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE buzhou_message (
                        id                VARCHAR(64)  PRIMARY KEY,
                        session_id        VARCHAR(128) NOT NULL,
                        turn_seq          INT          NOT NULL,
                        seq_in_turn       INT          NOT NULL,
                        role              VARCHAR(16)  NOT NULL,
                        content           LONGTEXT,
                        tool_calls        LONGTEXT,
                        tool_call_id      VARCHAR(64),
                        reasoning_content LONGTEXT,
                        metadata          LONGTEXT,
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
