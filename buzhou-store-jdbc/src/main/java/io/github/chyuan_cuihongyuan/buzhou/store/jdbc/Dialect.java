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
}
