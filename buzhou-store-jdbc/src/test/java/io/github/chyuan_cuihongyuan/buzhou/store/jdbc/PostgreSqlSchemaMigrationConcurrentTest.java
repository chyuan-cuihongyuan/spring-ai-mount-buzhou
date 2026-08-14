package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ticket 31 / spec 13 §stores-5：PG（Testcontainers，无 Docker 自动跳过）——
 * 双实例并发冷启动经 pg_advisory_lock 串行化后不炸、版本行恰好一套。
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlSchemaMigrationConcurrentTest {

    /** 双实例并发迁移的等待上限（秒）：覆盖 advisory lock 排队 + 一次全量建库。 */
    private static final long CONCURRENT_MIGRATION_TIMEOUT_SECONDS = 60L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Test
    void shouldSerializeAndStayGreen_whenTwoInstancesMigrateConcurrently() throws Exception {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // 两实例同时冷启动：advisory lock 串行化，后到者只见版本行、全无操作
            List<Future<Integer>> results = List.of(
                    pool.submit(() -> SchemaMigrator.migrate(dataSource, Dialect.POSTGRESQL)),
                    pool.submit(() -> SchemaMigrator.migrate(dataSource, Dialect.POSTGRESQL)));
            for (Future<Integer> result : results) {
                assertThat(result.get(CONCURRENT_MIGRATION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                        .isEqualTo(2);
            }
        } finally {
            pool.shutdownNow();
        }

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForList(
                "SELECT version FROM buzhou_schema_version ORDER BY version", Integer.class))
                .containsExactly(1, 2);
    }
}
