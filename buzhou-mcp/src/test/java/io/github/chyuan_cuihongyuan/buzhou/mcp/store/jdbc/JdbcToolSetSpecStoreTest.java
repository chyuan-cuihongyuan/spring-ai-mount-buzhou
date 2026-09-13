package io.github.chyuan_cuihongyuan.buzhou.mcp.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JdbcToolSetSpecStore 直测（spec 1200 / T1801 / K 会话 R1 补测——此前零覆盖）。
 *
 * <p>H2 内存库（H2StoresContractTest 同型，零 Docker 秒级跑）：断言 DDL 懒建表幂等、
 * replaceAll 整表替换语义、loadAll 按 name 排序与 JSON round-trip 保真（含超时/env/绑定）。
 * 与 DbToolSetProviderTest 的整表替换断言口径对齐。
 */
class JdbcToolSetSpecStoreTest {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(9);

    private JdbcToolSetSpecStore store;

    @BeforeEach
    void setUp() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:toolset-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        store = new JdbcToolSetSpecStore(new JdbcTemplate(ds));
    }

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STDIO, "cmd-" + name,
                Map.of("ROOT", "/tmp/" + name), CONNECT_TIMEOUT, REQUEST_TIMEOUT,
                Set.of(new ToolSetSpec.Binding("app-1", "agent-" + name)));
    }

    @Test
    void loadAllOnFreshStoreLazilyCreatesSchemaAndReturnsEmpty() {
        // 首次调用触发 ensureSchema（DDL 懒执行）；空库 → 空清单
        assertThat(store.loadAll()).isEmpty();
        // schemaReady 幂等：二次操作不重复建表且照常工作
        assertThat(store.loadAll()).isEmpty();
    }

    @Test
    void replaceAllPersistsSortedWithRoundTripFidelity() {
        store.replaceAll(List.of(spec("b-tool"), spec("a-tool")));

        List<ToolSetSpec> loaded = store.loadAll();
        assertThat(loaded).extracting(ToolSetSpec::name).containsExactly("a-tool", "b-tool");

        ToolSetSpec first = loaded.getFirst();
        assertThat(first.transport()).isEqualTo(Transport.STDIO);
        assertThat(first.endpoint()).isEqualTo("cmd-a-tool");
        assertThat(first.env()).containsExactlyEntriesOf(Map.of("ROOT", "/tmp/a-tool"));
        assertThat(first.connectTimeout()).isEqualTo(CONNECT_TIMEOUT);
        assertThat(first.requestTimeout()).isEqualTo(REQUEST_TIMEOUT);
        assertThat(first.bindings())
                .containsExactly(new ToolSetSpec.Binding("app-1", "agent-a-tool"));
    }

    @Test
    void replaceAllReplacesWholeTableNotMerge() {
        store.replaceAll(List.of(spec("old-a"), spec("old-b")));
        store.replaceAll(List.of(spec("new-c")));

        assertThat(store.loadAll()).extracting(ToolSetSpec::name).containsExactly("new-c");
    }

    @Test
    void replaceAllWithEmptyListClearsTable() {
        store.replaceAll(List.of(spec("x")));
        assertThat(store.loadAll()).hasSize(1);

        store.replaceAll(List.of());
        assertThat(store.loadAll()).isEmpty();
    }
}
