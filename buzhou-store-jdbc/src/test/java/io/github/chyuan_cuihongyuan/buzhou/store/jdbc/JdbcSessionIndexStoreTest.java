package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话索引 JDBC 实现（V3 迁移表）行为测试（spec 30 / T109 / impl-84）：
 * upsert 幂等收敛 / 过滤组合 / lastActive 倒序 / delete。
 */
class JdbcSessionIndexStoreTest {

    private final JdbcSessionIndexStore index;

    JdbcSessionIndexStoreTest() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:idx-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        JdbcBuzhouStores.createWithRecovery(dataSource, Dialect.H2); // 触发 V1–V3 迁移
        index = new JdbcSessionIndexStore(new JdbcTemplate(dataSource));
    }

    private static SessionInfo info(String sessionId, String appId, String status,
            long lastActive, Map<String, String> tags) {
        return new SessionInfo(sessionId, appId, "agent-1", status, 1000L, lastActive, 1, tags);
    }

    /** upsert 重复收敛 + 按 appId/status 过滤 + lastActive 倒序。 */
    @Test
    void upsertFiltersAndOrdering() {
        index.upsert(info("s-old", "app-a", SessionInfo.STATUS_ACTIVE, 1000L, Map.of()));
        index.upsert(info("s-new", "app-a", SessionInfo.STATUS_ACTIVE, 3000L, Map.of()));
        index.upsert(info("s-closed", "app-a", SessionInfo.STATUS_CLOSED, 2000L, Map.of()));
        index.upsert(info("s-b", "app-b", SessionInfo.STATUS_ACTIVE, 4000L, Map.of()));
        // 重复 upsert（生命周期点多次写同会话）：收敛为最新行
        index.upsert(info("s-old", "app-a", SessionInfo.STATUS_ACTIVE, 5000L, Map.of()));

        List<SessionInfo> result = index.list(new SessionIndexQuery(
                "app-a", null, SessionInfo.STATUS_ACTIVE, null, null, 0, 10));
        assertThat(result).extracting(SessionInfo::sessionId)
                .containsExactly("s-old", "s-new"); // lastActive 倒序，s-old 刷到 5000

        assertThat(index.get("s-closed"))
                .hasValueSatisfying(i -> assertThat(i.status()).isEqualTo(SessionInfo.STATUS_CLOSED));
    }

    /** tag 过滤（JSON 列 LIKE + 内存精确复核）；前缀邻键不误报。 */
    @Test
    void tagFilterExactMatch() {
        index.upsert(info("t-1", "app", SessionInfo.STATUS_ACTIVE, 1000L, Map.of("env", "prod")));
        index.upsert(info("t-2", "app", SessionInfo.STATUS_ACTIVE, 2000L, Map.of("envx", "prod")));

        List<SessionInfo> result = index.list(new SessionIndexQuery(
                null, null, null, "env", "prod", 0, 10));
        assertThat(result).extracting(SessionInfo::sessionId).containsExactly("t-1");
    }

    /** delete 摘除索引行（幂等）。 */
    @Test
    void deleteRemovesRow() {
        index.upsert(info("gone", "app", SessionInfo.STATUS_ACTIVE, 1000L, Map.of()));
        index.delete("gone");
        index.delete("gone"); // 幂等
        assertThat(index.get("gone")).isEmpty();
        assertThat(index.list(SessionIndexQuery.defaults())).isEmpty();
    }
}
