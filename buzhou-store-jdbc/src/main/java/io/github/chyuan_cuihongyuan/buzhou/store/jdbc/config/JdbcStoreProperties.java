package io.github.chyuan_cuihongyuan.buzhou.store.jdbc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JDBC store 装配属性（spec 08 / 09 / ticket 22，前缀 {@code buzhou.store.jdbc}）。
 *
 * @param dialect SQL 方言：{@code H2} / {@code MYSQL} / {@code POSTGRESQL}（默认 {@code H2}）
 */
@ConfigurationProperties(prefix = "buzhou.store.jdbc")
public record JdbcStoreProperties(String dialect) {

    public JdbcStoreProperties {
        dialect = (dialect == null || dialect.isBlank()) ? "H2" : dialect.toUpperCase();
    }
}
