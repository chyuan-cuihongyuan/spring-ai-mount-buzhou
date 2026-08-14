package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

/**
 * 支持的 SQL 方言（ticket 31 / spec 13 §stores-5）：决定版本化迁移脚本的查找目录
 * （{@code classpath*:db/migration/<目录>/V<n>__<描述>.sql}）。
 *
 * <p>schema 建库一律经 {@link SchemaMigrator} 的版本化脚本执行（V1 基线取代了旧的
 * {@code db/schema-<dialect>.sql} 单文件建库方式）。
 */
public enum Dialect {
    H2("h2"),
    MYSQL("mysql"),
    POSTGRESQL("postgresql");

    private final String migrationDirectory;

    Dialect(String migrationDirectory) {
        this.migrationDirectory = migrationDirectory;
    }

    /** 版本化迁移脚本目录名（{@code db/migration/} 之下）。 */
    public String migrationDirectory() {
        return migrationDirectory;
    }

    /**
     * 按 {@link java.sql.DatabaseMetaData#getDatabaseProductName()} 自动探测方言
     * （impl-42 / spec 13 §T68：{@code buzhou.store.jdbc.dialect} 缺省不再假定为 H2——
     * 连接是什么库就用什么方言）。产品名不认识时抛出带指引的 IllegalStateException。
     */
    public static Dialect detect(javax.sql.DataSource dataSource) {
        String productName;
        try (java.sql.Connection connection = dataSource.getConnection()) {
            productName = connection.getMetaData().getDatabaseProductName();
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("方言自动探测失败（连接不可用）——"
                    + "请显式配置 buzhou.store.jdbc.dialect=H2|MYSQL|POSTGRESQL", e);
        }
        String normalized = productName == null ? "" : productName.toLowerCase();
        if (normalized.contains("h2")) {
            return H2;
        }
        if (normalized.contains("mysql") || normalized.contains("mariadb")) {
            return MYSQL;
        }
        if (normalized.contains("postgres")) {
            return POSTGRESQL;
        }
        throw new IllegalStateException("不支持的数据库（产品名 \"" + productName
                + "\"）——支持 H2/MYSQL/POSTGRESQL；请检查连接指向或显式配置 dialect");
    }
}
