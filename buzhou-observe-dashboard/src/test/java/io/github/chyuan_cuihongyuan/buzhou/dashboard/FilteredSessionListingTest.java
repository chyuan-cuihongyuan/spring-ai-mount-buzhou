package io.github.chyuan_cuihongyuan.buzhou.dashboard;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * dashboard 过滤会话列表测试（spec 36 §B / T122 / impl-97）：索引装配时按
 * app/status/tag 过滤 + 分页；未装配回退观测留痕（fromIndex=false 降级可感）。
 */
class FilteredSessionListingTest {

    /** 索引源：过滤组合命中 + fromIndex=true。 */
    @Test
    void filteredListingServedByIndex() {
        InMemorySessionIndexStore index = new InMemorySessionIndexStore();
        index.upsert(new SessionInfo("s-1", "app-a", "ag", SessionInfo.STATUS_ACTIVE,
                1L, 3000L, 1, Map.of("env", "prod")));
        index.upsert(new SessionInfo("s-2", "app-a", "ag", SessionInfo.STATUS_CLOSED,
                1L, 2000L, 5, Map.of("env", "prod")));
        index.upsert(new SessionInfo("s-3", "app-b", "ag", SessionInfo.STATUS_ACTIVE,
                1L, 1000L, 2, Map.of()));
        DashboardQueryService service = new DashboardQueryService(new InMemoryObservabilityStore(), index);

        DashboardQueryService.IndexedSessionPage page =
                service.listSessionsFiltered("app-a", null, null, "env", "prod", null, 10);

        assertThat(page.fromIndex()).isTrue();
        assertThat(page.items()).extracting(SessionInfo::sessionId).containsExactly("s-1", "s-2");
        assertThat(page.nextCursor()).isNull();

        // 分页探测：size=1 时首页 1 条 + nextCursor 指向下一页
        DashboardQueryService.IndexedSessionPage first =
                service.listSessionsFiltered("app-a", null, null, "env", "prod", null, 1);
        assertThat(first.items()).singleElement().extracting(SessionInfo::sessionId).isEqualTo("s-1");
        assertThat(first.nextCursor()).isEqualTo("1");
        DashboardQueryService.IndexedSessionPage second =
                service.listSessionsFiltered("app-a", null, null, "env", "prod", "1", 1);
        assertThat(second.items()).singleElement().extracting(SessionInfo::sessionId).isEqualTo("s-2");
        assertThat(second.nextCursor()).isNull();
    }

    /** 无索引回退：观测留痕映射行 + fromIndex=false（调用方感知降级）。 */
    @Test
    void fallbackToObservabilityWithoutIndex() {
        InMemoryObservabilityStore observability = new InMemoryObservabilityStore();
        observability.saveSpans(java.util.List.of(new io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord(
                "sp-1", null, "obs-session", 1, "SESSION", "session",
                java.time.Instant.ofEpochMilli(1000), java.time.Instant.ofEpochMilli(2000),
                "OK", Map.of())));
        DashboardQueryService service = new DashboardQueryService(observability, null);

        DashboardQueryService.IndexedSessionPage page =
                service.listSessionsFiltered("app-a", null, null, null, null, null, 10);

        assertThat(page.fromIndex()).isFalse(); // 降级可感
        assertThat(page.items()).singleElement()
                .satisfies(info -> assertThat(info.sessionId()).isEqualTo("obs-session"));
    }
}
