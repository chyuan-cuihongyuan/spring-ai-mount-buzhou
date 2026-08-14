package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 版本化 schema 迁移管理器（ticket 31 / spec 13 §stores-5）：Flyway 思想的自建零依赖实现，
 * 使 JDBC 部署「可升级、可重复启动、可多实例冷启动」。
 *
 * <p><b>约定</b>：迁移脚本位于 {@code classpath*:db/migration/<方言目录>/V<n>__<描述>.sql}
 * （目录见 {@link Dialect#migrationDirectory()}），按 {@code <n>} 升序执行；已应用版本记录在
 * {@code buzhou_schema_version} 表（version 主键 + description + applied_at）。
 *
 * <p><b>启动流程</b>：
 * <ol>
 *   <li>并发保护：PG {@code pg_advisory_lock} / MySQL {@code GET_LOCK}（H2 内嵌单进程无需锁）；</li>
 *   <li>幂等建版本表；</li>
 *   <li><b>基线判定</b>：库中已存在业务表（{@code buzhou_message}）但无版本行 —— 判定为
 *       迁移机制上线前的旧库，直接写入基线版本行（= 首个脚本版本），不重跑脚本；</li>
 *   <li>空库 → 从头执行全部脚本；版本落后 → 依次执行未应用脚本。
 *       每脚本独立事务（脚本 + 版本行同进同退；MySQL DDL 隐式提交，靠脚本自身幂等兜底）。</li>
 * </ol>
 *
 * <p>该类线程安全（无共享可变状态），多个数据源可各持一个实例并行迁移。
 */
public final class SchemaMigrator {

    /** 版本表名。 */
    static final String VERSION_TABLE = "buzhou_schema_version";

    /** 基线探测锚点表：V1 基线中最先创建的业务表，存在即视为「迁移机制之前的旧库」。 */
    private static final String BASELINE_ANCHOR_TABLE = "buzhou_message";

    /** 并发锁名（MySQL GET_LOCK 用；PG advisory lock 键也由它派生，避免魔法数字）。 */
    private static final String LOCK_NAME = "buzhou_schema_migration";

    /** PG advisory lock 键：由锁名散列派生（int 值域内，碰撞概率可忽略且仅影响互斥名空间）。 */
    private static final long PG_ADVISORY_LOCK_KEY = LOCK_NAME.hashCode();

    /** MySQL GET_LOCK 等待秒数：覆盖另一实例完成一次冷启动迁移的常规时长。 */
    private static final int MYSQL_LOCK_TIMEOUT_SECONDS = 10;

    /** MySQL GET_LOCK 成功返回值（1 = 获得；0 = 超时；NULL = 出错）。 */
    private static final int MYSQL_LOCK_ACQUIRED = 1;

    /** 版本表尚无任何版本行时的哨兵值。 */
    private static final int NO_VERSION = 0;

    /** 迁移脚本文件名约定：V<版本>__<描述>.sql。 */
    private static final Pattern SCRIPT_NAME_PATTERN = Pattern.compile("V(\\d+)__(.+)\\.sql");

    private static final String BASELINE_DESCRIPTION = "baseline: adopted pre-migration schema";

    private final Dialect dialect;

    /**
     * 按方言创建迁移器。
     *
     * @param dialect 目标库方言
     */
    public SchemaMigrator(Dialect dialect) {
        this.dialect = dialect;
    }

    /**
     * 便捷入口：按方言执行迁移并返回迁移后的 schema 版本。
     *
     * @param dataSource 目标数据源
     * @param dialect    方言
     * @return 迁移后的 schema 版本（当前已应用的最大版本号）
     */
    public static int migrate(DataSource dataSource, Dialect dialect) {
        return new SchemaMigrator(dialect).migrate(dataSource);
    }

    /**
     * 执行迁移：加锁 → 建版本表 → 基线判定 → 依次应用未执行的脚本。
     *
     * @param dataSource 目标数据源
     * @return 迁移后的 schema 版本（当前已应用的最大版本号）
     * @throws IllegalStateException 迁移脚本执行失败、脚本约定被破坏或并发锁不可得时
     */
    public int migrate(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            MigrationLock lock = acquireLock(connection);
            try {
                ensureVersionTable(connection);
                return applyPending(connection, discoverMigrations());
            } finally {
                lock.release(connection);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Buzhou schema 迁移失败(dialect=%s)".formatted(dialect), e);
        }
    }

    // ---------- 版本表 ----------

    private void ensureVersionTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(versionTableDdl());
        }
    }

    private String versionTableDdl() {
        return switch (dialect) {
            // MySQL 的列内 PRIMARY KEY 写法与其余两方的列约束写法等价
            case MYSQL -> """
                    CREATE TABLE IF NOT EXISTS %s (
                        version      INT          NOT NULL,
                        description  VARCHAR(256) NOT NULL,
                        applied_at   TIMESTAMP    NOT NULL,
                        PRIMARY KEY (version)
                    )""".formatted(VERSION_TABLE);
            case H2, POSTGRESQL -> """
                    CREATE TABLE IF NOT EXISTS %s (
                        version      INT          PRIMARY KEY,
                        description  VARCHAR(256) NOT NULL,
                        applied_at   TIMESTAMP    NOT NULL
                    )""".formatted(VERSION_TABLE);
        };
    }

    private int maxAppliedVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT MAX(version) FROM " + VERSION_TABLE)) {
            rs.next();
            return rs.getObject(1) == null ? NO_VERSION : rs.getInt(1);
        }
    }

    private void insertVersion(Connection connection, int version, String description) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO %s (version, description, applied_at) VALUES (?,?,?)".formatted(VERSION_TABLE))) {
            ps.setInt(1, version);
            ps.setString(2, description);
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        }
    }

    // ---------- 基线判定与脚本应用 ----------

    private int applyPending(Connection connection, List<Migration> migrations) throws SQLException {
        int maxApplied = maxAppliedVersion(connection);
        if (maxApplied == NO_VERSION && businessTableExists(connection)) {
            // 基线判定：业务表已在而版本行全无 → 旧库（由老版 schema-*.sql 建成）。
            // 采纳首个脚本版本为基线行，不重跑建表脚本；其后的增量脚本（如 V2）照常升级。
            Migration baseline = migrations.getFirst();
            insertVersion(connection, baseline.version(), BASELINE_DESCRIPTION);
            maxApplied = baseline.version();
        }
        int latest = maxApplied;
        for (Migration migration : migrations) {
            if (migration.version() > maxApplied) {
                apply(connection, migration);
                latest = migration.version();
            }
        }
        return latest;
    }

    private void apply(Connection connection, Migration migration) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            ScriptUtils.executeSqlScript(connection, migration.script());
            insertVersion(connection, migration.version(), migration.description());
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw new IllegalStateException(
                    "Buzhou 迁移脚本执行失败: %s(dialect=%s)".formatted(migration, dialect), e);
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private boolean businessTableExists(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        // H2 默认将未加引号标识符存为大写、PG/MySQL 存为小写，两种形态都探测
        return tableExists(metaData, BASELINE_ANCHOR_TABLE) || tableExists(metaData, BASELINE_ANCHOR_TABLE.toUpperCase());
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet tables = metaData.getTables(null, null, tableName, new String[] {"TABLE"})) {
            return tables.next();
        }
    }

    // ---------- 脚本发现 ----------

    private List<Migration> discoverMigrations() {
        String location = "classpath*:db/migration/" + dialect.migrationDirectory() + "/V*.sql";
        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(location);
        } catch (IOException e) {
            throw new IllegalStateException("无法扫描 Buzhou 迁移脚本: " + location, e);
        }
        if (resources.length == 0) {
            throw new IllegalStateException("未找到任何 Buzhou 迁移脚本: " + location);
        }
        List<Migration> migrations = new ArrayList<>();
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            var matcher = SCRIPT_NAME_PATTERN.matcher(fileName == null ? "" : fileName);
            if (!matcher.matches()) {
                throw new IllegalStateException(
                        "迁移脚本命名不符合 V<n>__<描述>.sql 约定: " + fileName + " @ " + location);
            }
            migrations.add(new Migration(Integer.parseInt(matcher.group(1)),
                    matcher.group(2).replace('_', ' '), resource));
        }
        migrations.sort(Comparator.comparingInt(Migration::version));
        for (int i = 1; i < migrations.size(); i++) {
            if (migrations.get(i).version() == migrations.get(i - 1).version()) {
                throw new IllegalStateException("迁移脚本版本号重复: V" + migrations.get(i).version()
                        + " @ " + location);
            }
        }
        return migrations;
    }

    // ---------- 并发保护 ----------

    private MigrationLock acquireLock(Connection connection) throws SQLException {
        return switch (dialect) {
            // H2 内嵌单进程，无并发冷启动场景，无需锁
            case H2 -> MigrationLock.NOOP;
            case POSTGRESQL -> {
                executeLockQuery(connection, "SELECT pg_advisory_lock(?)", PG_ADVISORY_LOCK_KEY);
                yield connection2 -> executeLockQuery(connection2, "SELECT pg_advisory_unlock(?)",
                        PG_ADVISORY_LOCK_KEY);
            }
            case MYSQL -> {
                try (PreparedStatement ps = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
                    ps.setString(1, LOCK_NAME);
                    ps.setInt(2, MYSQL_LOCK_TIMEOUT_SECONDS);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) != MYSQL_LOCK_ACQUIRED) {
                            throw new IllegalStateException(
                                    "获取 MySQL 迁移锁失败(锁=" + LOCK_NAME + ", 等待秒数="
                                            + MYSQL_LOCK_TIMEOUT_SECONDS + ")");
                        }
                    }
                }
                yield connection2 -> executeStringLockQuery(connection2, "SELECT RELEASE_LOCK(?)", LOCK_NAME);
            }
        };
    }

    private static void executeLockQuery(Connection connection, String sql, long key) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
            }
        }
    }

    private static void executeStringLockQuery(Connection connection, String sql, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
            }
        }
    }

    /** 方言相关的迁移互斥锁句柄（在迁移结束时释放）。 */
    private interface MigrationLock {

        /** 空实现（H2）。 */
        MigrationLock NOOP = connection -> {
        };

        /** 释放锁（吞不下异常由调用方统一包装为迁移失败）。 */
        void release(Connection connection) throws SQLException;
    }

    /** 一个待执行的版本化迁移脚本。 */
    private record Migration(int version, String description, Resource script) {

        @Override
        public String toString() {
            return "V" + version + "__" + description.replace(' ', '_') + ".sql";
        }
    }
}
