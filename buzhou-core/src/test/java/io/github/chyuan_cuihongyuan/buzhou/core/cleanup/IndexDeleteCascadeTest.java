package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.SessionIndexObserver;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 索引删除联动 + fsck 索引源（spec 33 §B / T113 / impl-88）：会话 delete() 级联把索引行
 * 置 DELETED（审计留存）；fsck 有索引时全集走索引源、无索引回退观测。
 */
class IndexDeleteCascadeTest {

    /** delete() 级联置 DELETED（非物理删）；索引未见过的会话删除为无操作。 */
    @Test
    void deleteMarksIndexRowDeleted() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        InMemorySessionIndexStore index = new InMemorySessionIndexStore();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.merge(
                RuntimeConfig.defaults(),
                RuntimeConfig.assemblyCustomizers(List.of(SessionIndexObserver.wiring(index))),
                RuntimeConfig.cleanupContributors(List.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleanupContributor.of(
                                "session-index", sessionId -> index.get(sessionId)
                                        .ifPresent(info -> index.upsert(new SessionInfo(
                                                info.sessionId(), info.appId(), info.agentName(),
                                                SessionInfo.STATUS_DELETED, info.createdAtEpochMs(),
                                                info.lastActiveAtEpochMs(), info.turnCount(),
                                                info.tags()))))))));
        AgentSession session = runtime.spawn("app", "ag", "del-1");
        session.chat("q");
        assertThat(index.get("del-1"))
                .hasValueSatisfying(i -> assertThat(i.status()).isEqualTo(SessionInfo.STATUS_ACTIVE));

        session.delete();

        assertThat(stores.messageStore().load("del-1")).isEmpty(); // 级联清干净
        assertThat(index.get("del-1"))
                .hasValueSatisfying(i -> assertThat(i.status()).isEqualTo(SessionInfo.STATUS_DELETED));
    }

    /** fsck 索引源：索引行覆盖观测未留痕的会话（全集更完整）；索引滞后时回退观测源。 */
    @Test
    void fsckPrefersIndexUniverseWithFallback() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        // 会话有孤儿摘要但观测未留痕（observability 为空）
        stores.summaryStore().save("no-obs", new StructuredSummary(
                "no-obs", 1, Map.of(), 1, Instant.now()));
        stores.sessionStateStore().put("residue", new StateEntry("k", "v", "t", 1, null, Instant.now()));

        // 无索引：回退观测源 → 观测为空，extras 才能补
        StoreIntegrityReport viaObservability = StoreFsck.run(stores, java.util.Set.of("no-obs"));
        assertThat(viaObservability.count(StoreIntegrityReport.ORPHAN_SUMMARY)).isEqualTo(1);

        // 有索引：索引行即全集（无需 extras）
        InMemorySessionIndexStore index = new InMemorySessionIndexStore();
        index.upsert(new SessionInfo("no-obs", "app", "ag", SessionInfo.STATUS_ACTIVE,
                1L, 2L, 0, Map.of()));
        index.upsert(new SessionInfo("residue", "app", "ag", SessionInfo.STATUS_ACTIVE,
                1L, 2L, 0, Map.of()));

        StoreIntegrityReport viaIndex = StoreFsck.run(stores, index, java.util.Set.of());
        assertThat(viaIndex.count(StoreIntegrityReport.ORPHAN_SUMMARY)).isEqualTo(1);
        assertThat(viaIndex.count(StoreIntegrityReport.STATE_RESIDUE)).isEqualTo(1);

        // 索引滞后（行数少于观测留痕）：回退观测源不丢会话
        InMemorySessionIndexStore stale = new InMemorySessionIndexStore();
        stores.observabilityStore().saveSpans(List.of(new SpanRecord(
                "s-obs-only", null, "obs-only", 1, "SESSION", "session", Instant.now(),
                Instant.now(), "OK", Map.of())));
        stores.messageStore().append("obs-only", List.of(new BuzhouMessage(
                "m-1", "obs-only", 1, 1, Role.USER, "q", List.of(), null, null, null,
                Map.of(), Instant.now())));
        StoreIntegrityReport fallback = StoreFsck.run(stores, stale, java.util.Set.of());
        assertThat(fallback.clean()).isTrue(); // 观测源全集覆盖 obs-only（有消息不误报）
    }

    /** 索引查询契约回访：DELETED 行仅在显式 status 过滤时可见（默认列表不含）。 */
    @Test
    void deletedRowsVisibleOnlyByExplicitFilter() {
        InMemorySessionIndexStore index = new InMemorySessionIndexStore();
        index.upsert(new SessionInfo("gone", "app", "ag", SessionInfo.STATUS_DELETED,
                1L, 2L, 1, Map.of()));
        index.upsert(new SessionInfo("live", "app", "ag", SessionInfo.STATUS_ACTIVE,
                1L, 3L, 1, Map.of()));

        assertThat(index.list(SessionIndexQuery.defaults()))
                .singleElement().extracting(SessionInfo::sessionId).isEqualTo("live");
        assertThat(index.list(new SessionIndexQuery(
                null, null, SessionInfo.STATUS_DELETED, null, null, 0, 10)))
                .singleElement().extracting(SessionInfo::sessionId).isEqualTo("gone");
    }
}
