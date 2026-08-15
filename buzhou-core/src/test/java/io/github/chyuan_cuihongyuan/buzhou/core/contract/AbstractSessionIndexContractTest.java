package io.github.chyuan_cuihongyuan.buzhou.core.contract;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SessionIndexStore 共享契约套件（spec 33 §A / T112 / impl-87）：三实现（内存 / JDBC /
 * Redis）语义等价的契约矩阵——upsert 幂等收敛、get、过滤组合与 lastActive 倒序、
 * tag 精确匹配、delete 幂等、持久语义（实现自定）。实现私有细节留在各自模块测试。
 */
public abstract class AbstractSessionIndexContractTest {

    /** 被测索引（每用例前由实现侧重建或清场）。 */
    protected abstract SessionIndexStore index();

    /** 用例间清场（幂等）。 */
    @AfterEach
    protected abstract void cleanUp();

    private static SessionInfo info(String sessionId, String appId, String agent, String status,
            long lastActive, Map<String, String> tags) {
        return new SessionInfo(sessionId, appId, agent, status, 1_000L, lastActive, 1, tags);
    }

    /** upsert 幂等收敛：同会话多次生命周期写收敛为最新行。 */
    @Test
    public void upsertConvergesToLatestRow() {
        SessionIndexStore index = index();
        index.upsert(info("c-1", "app-a", "ag", SessionInfo.STATUS_ACTIVE, 1_000L, Map.of()));
        index.upsert(info("c-1", "app-a", "ag", SessionInfo.STATUS_CLOSED, 5_000L, Map.of("k", "v")));

        assertThat(index.get("c-1")).hasValueSatisfying(i -> {
            assertThat(i.status()).isEqualTo(SessionInfo.STATUS_CLOSED);
            assertThat(i.lastActiveAtEpochMs()).isEqualTo(5_000L);
            assertThat(i.tags()).containsEntry("k", "v");
        });
    }

    /** 过滤组合与排序：appId/agentName/status 精确过滤 + lastActive 倒序 + 分页。 */
    @Test
    public void listFiltersAndOrdersByLastActiveDesc() {
        SessionIndexStore index = index();
        index.upsert(info("c-old", "app-a", "ag-1", SessionInfo.STATUS_ACTIVE, 1_000L, Map.of()));
        index.upsert(info("c-mid", "app-a", "ag-1", SessionInfo.STATUS_CLOSED, 2_000L, Map.of()));
        index.upsert(info("c-new", "app-a", "ag-2", SessionInfo.STATUS_ACTIVE, 3_000L, Map.of()));
        index.upsert(info("c-b", "app-b", "ag-1", SessionInfo.STATUS_ACTIVE, 4_000L, Map.of()));

        List<SessionInfo> appAActive = index.list(new SessionIndexQuery(
                "app-a", null, SessionInfo.STATUS_ACTIVE, null, null, 0, 10));
        assertThat(appAActive).extracting(SessionInfo::sessionId)
                .containsExactly("c-new", "c-old"); // 倒序；CLOSED/他 app 不入

        List<SessionInfo> agentScoped = index.list(new SessionIndexQuery(
                null, "ag-1", null, null, null, 0, 10));
        assertThat(agentScoped).extracting(SessionInfo::sessionId)
                .containsExactly("c-b", "c-mid", "c-old");

        List<SessionInfo> paged = index.list(new SessionIndexQuery(
                null, null, null, null, null, 1, 2));
        assertThat(paged).extracting(SessionInfo::sessionId)
                .containsExactly("c-new", "c-mid"); // 偏移 1 取 2（全集倒序 c-b,c-new,c-mid,c-old）
    }

    /** tag 精确匹配：键值对过滤；前缀邻键不误报。 */
    @Test
    public void tagFilterMatchesExactly() {
        SessionIndexStore index = index();
        index.upsert(info("t-1", "app", "ag", SessionInfo.STATUS_ACTIVE, 1_000L, Map.of("env", "prod")));
        index.upsert(info("t-2", "app", "ag", SessionInfo.STATUS_ACTIVE, 2_000L, Map.of("envx", "prod")));
        index.upsert(info("t-3", "app", "ag", SessionInfo.STATUS_ACTIVE, 3_000L, Map.of("env", "staging")));

        List<SessionInfo> prod = index.list(new SessionIndexQuery(
                null, null, null, "env", "prod", 0, 10));
        assertThat(prod).extracting(SessionInfo::sessionId).containsExactly("t-1");
    }

    /** delete 幂等摘行。 */
    @Test
    public void deleteIsIdempotent() {
        SessionIndexStore index = index();
        index.upsert(info("d-1", "app", "ag", SessionInfo.STATUS_ACTIVE, 1_000L, Map.of()));

        index.delete("d-1");
        index.delete("d-1");

        assertThat(index.get("d-1")).isEmpty();
        assertThat(index.list(SessionIndexQuery.defaults())).isEmpty();
    }

    /** 空查询：全量倒序（默认页）。 */
    @Test
    public void emptyIndexListsNothing() {
        assertThat(index().list(SessionIndexQuery.defaults())).isEmpty();
    }

    /** 保留策略清扫（spec 37 §C）：过期 CLOSED/DELETED 淘汰、ACTIVE 永不扫、limit 截断。 */
    @Test
    public void purgeEvictsExpiredNonActiveRowsOnly() {
        SessionIndexStore index = index();
        index.upsert(info("p-live", "app", "ag", SessionInfo.STATUS_ACTIVE, 100L, Map.of())); // 老但活跃
        index.upsert(info("p-closed", "app", "ag", SessionInfo.STATUS_CLOSED, 100L, Map.of())); // 老且关闭
        index.upsert(info("p-dead", "app", "ag", SessionInfo.STATUS_DELETED, 100L, Map.of())); // 老且已删
        index.upsert(info("p-fresh", "app", "ag", SessionInfo.STATUS_CLOSED,
                System.currentTimeMillis(), Map.of())); // 新关闭（未过期）

        int purged = index.purgeOlderThan(java.time.Instant.ofEpochMilli(1_000L), 10);

        assertThat(purged).isEqualTo(2);
        assertThat(index.get("p-live")).isPresent(); // ACTIVE 永不扫
        assertThat(index.get("p-fresh")).isPresent(); // 未过期
        assertThat(index.get("p-closed")).isEmpty();
        assertThat(index.get("p-dead")).isEmpty();
    }

    /** 默认排除 DELETED（审计行仅显式 status 过滤可见——spec 33 §B）。 */
    @Test
    public void deletedRowsHiddenFromDefaultListing() {
        SessionIndexStore index = index();
        index.upsert(info("x-live", "app", "ag", SessionInfo.STATUS_ACTIVE, 1_000L, Map.of()));
        index.upsert(info("x-gone", "app", "ag", SessionInfo.STATUS_DELETED, 2_000L, Map.of()));

        assertThat(index.list(SessionIndexQuery.defaults()))
                .singleElement().extracting(SessionInfo::sessionId).isEqualTo("x-live");
        assertThat(index.list(new SessionIndexQuery(
                null, null, SessionInfo.STATUS_DELETED, null, null, 0, 10)))
                .singleElement().extracting(SessionInfo::sessionId).isEqualTo("x-gone");
    }
}
