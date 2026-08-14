package io.github.chyuan_cuihongyuan.buzhou.store.jdbc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JDBC store 装配属性（spec 08/09 ticket 22 + spec 13 §stores-5 ticket 31，前缀 {@code buzhou.store.jdbc}）。
 *
 * @param dialect         SQL 方言：{@code H2} / {@code MYSQL} / {@code POSTGRESQL}；
 *                        impl-42 迁移：默认 {@code AUTO}（按连接 DatabaseMetaData 探测——
 *                        原缺省假定 H2，接错库会跑错脚本目录）；显式值不变
 * @param recoveryEnabled 是否装配恢复设施 bean（RunRegistry / ToolCallLog，默认 {@code true}；
 *                        与 {@code BuzhouJdbcStoreAutoConfiguration} 上的同名条件开关一致）
 */
@ConfigurationProperties(prefix = "buzhou.store.jdbc")
public record JdbcStoreProperties(String dialect, Boolean recoveryEnabled) {

    public JdbcStoreProperties {
        dialect = (dialect == null || dialect.isBlank()) ? "AUTO" : dialect.trim().toUpperCase();
        recoveryEnabled = (recoveryEnabled == null) || recoveryEnabled;
    }
}
