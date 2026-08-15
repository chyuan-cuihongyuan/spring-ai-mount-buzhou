package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.contract.AbstractSessionIndexContractTest;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JDBC 实现契约接入（H2 内嵌）+ 持久语义（新 store 实例同数据源 = 重启后索引仍在）。
 */
class H2SessionIndexContractTest extends AbstractSessionIndexContractTest {

    private final JdbcDataSource dataSource;
    private final JdbcSessionIndexStore store;

    H2SessionIndexContractTest() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:idx-contract-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        JdbcBuzhouStores.createWithRecovery(dataSource, Dialect.H2); // V1–V3 迁移
        store = new JdbcSessionIndexStore(new JdbcTemplate(dataSource));
    }

    @Override
    protected SessionIndexStore index() {
        return store;
    }

    @Override
    @AfterEach
    protected void cleanUp() {
        new JdbcTemplate(dataSource).update("DELETE FROM buzhou_session_index");
    }

    /** 持久语义：新 store 实例（模拟重启）同数据源可见全部行。 */
    @Test
    void rowsSurviveNewStoreInstanceOverSameDataSource() {
        store.upsert(new SessionInfo("persist-1", "app", "ag", SessionInfo.STATUS_ACTIVE,
                1L, System.currentTimeMillis(), 3, Map.of("k", "v")));

        JdbcSessionIndexStore restarted = new JdbcSessionIndexStore(new JdbcTemplate(dataSource));

        assertThat(restarted.get("persist-1")).isPresent();
        assertThat(restarted.list(new SessionIndexQuery(
                null, null, null, "k", "v", 0, 10)))
                .singleElement().extracting(SessionInfo::sessionId).isEqualTo("persist-1");
    }
}
