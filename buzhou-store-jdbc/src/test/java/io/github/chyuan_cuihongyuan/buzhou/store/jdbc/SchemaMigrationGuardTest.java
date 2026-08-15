package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 42 §A / T155 / impl-126：迁移器防护——未来版本拒绝（旧构建对上新库必须拒绝）与
 * 已应用脚本 checksum 校验（事后改脚本可检出）；存量 NULL checksum 行升级时回填锚定。
 */
class SchemaMigrationGuardTest {

    private static JdbcDataSource newDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:migration-guard-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        return dataSource;
    }

    @Test
    void futureSchemaVersionRejectedInsteadOfSilentPass() {
        JdbcDataSource dataSource = newDataSource();
        SchemaMigrator.migrate(dataSource, Dialect.H2);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // 模拟「新构建写过的库」：插入未来版本行 V999
        jdbc.update("INSERT INTO buzhou_schema_version (version, description, applied_at, checksum) "
                + "VALUES (999, 'future', CURRENT_TIMESTAMP, NULL)");

        // 旧构建（只认到 V3）对上 V999：静默通过 = 以旧 schema 写新库 → 必须拒绝
        assertThatThrownBy(() -> SchemaMigrator.migrate(dataSource, Dialect.H2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未来 schema 版本")
                .hasMessageContaining("999");
    }

    @Test
    void appliedScriptTamperingDetectedByChecksum() {
        JdbcDataSource dataSource = newDataSource();
        SchemaMigrator.migrate(dataSource, Dialect.H2);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // 迁移成功后每行都有 checksum 锚点（非空）
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM buzhou_schema_version WHERE checksum IS NOT NULL",
                Integer.class)).isEqualTo(3);

        // 模拟「已应用脚本被事后改动」：把 V2 行的 checksum 改成别的值
        jdbc.update("UPDATE buzhou_schema_version SET checksum='deadbeef' WHERE version=2");
        assertThatThrownBy(() -> SchemaMigrator.migrate(dataSource, Dialect.H2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已应用迁移脚本被改动")
                .hasMessageContaining("V2");

        // 恢复正确值后幂等通过
        jdbc.update("UPDATE buzhou_schema_version SET checksum=NULL WHERE version=2");
        int version = SchemaMigrator.migrate(dataSource, Dialect.H2);
        assertThat(version).isEqualTo(3);
        assertThat(jdbc.queryForObject(
                "SELECT checksum FROM buzhou_schema_version WHERE version=2", String.class))
                .isNotBlank(); // NULL 行回填锚定（首次升级即锚定，其后漂移可检）
    }
}
