package io.github.chyuan_cuihongyuan.buzhou.mcp.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JdbcToolSetSpecStore 真实 PostgreSQL 方言回归（T1804 / spec 1200：DDL 曾用 CLOB——
 * PG 无此类型，ensureSchema 必抛；修复为 TEXT 后在真实 PG 上锁住）。
 *
 * <p>与 store-jdbc 的 PostgreSqlStoresContractTest 同型：Testcontainers postgres:17，
 * 无 Docker 自动跳过（CI 有 Docker 常跑）。
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlToolSetSpecStoreTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    private static JdbcToolSetSpecStore storeOnRealPostgres() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return new JdbcToolSetSpecStore(new JdbcTemplate(dataSource));
    }

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STREAMABLE_HTTP, "https://mcp.example/" + name,
                Map.of("AUTH", "bearer"), Duration.ofSeconds(3), Duration.ofSeconds(9),
                Set.of(new ToolSetSpec.Binding("app-1", "agent-" + name)));
    }

    @Test
    void ensureSchemaAndRoundTripWorkOnRealPostgres() {
        JdbcToolSetSpecStore store = storeOnRealPostgres();

        // 修复前：CLOB 类型在 PG 不存在 → 首次 loadAll 即 BadSqlGrammar
        assertThat(store.loadAll()).isEmpty();

        store.replaceAll(List.of(spec("pg-b"), spec("pg-a")));
        List<ToolSetSpec> loaded = store.loadAll();
        assertThat(loaded).extracting(ToolSetSpec::name).containsExactly("pg-a", "pg-b");
        ToolSetSpec first = loaded.getFirst();
        assertThat(first.transport()).isEqualTo(Transport.STREAMABLE_HTTP);
        assertThat(first.env()).containsExactlyEntriesOf(Map.of("AUTH", "bearer"));
        assertThat(first.bindings())
                .containsExactly(new ToolSetSpec.Binding("app-1", "agent-pg-a"));

        // 整表替换语义在 PG 上同样成立
        store.replaceAll(List.of());
        assertThat(store.loadAll()).isEmpty();
    }
}
