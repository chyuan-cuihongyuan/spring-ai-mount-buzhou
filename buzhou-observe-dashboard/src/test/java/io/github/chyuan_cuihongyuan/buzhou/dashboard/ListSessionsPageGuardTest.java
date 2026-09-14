package io.github.chyuan_cuihongyuan.buzhou.dashboard;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1405 / T2112：查询页守卫（Grafana query limit 思想）——listSessions
 * 无钳制缺陷修复：size>200 钳制、size&lt;1 归 1（原负数 subList 异常）、
 * 游标格式非法抛可读 IAE（原裸 NFE）；钳制不破坏翻页语义。
 */
class ListSessionsPageGuardTest {

    private io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore store;
    private DashboardQueryService service;

    @BeforeEach
    void setUp() {
        store = Buzhou.inMemoryStores().observabilityStore();
        service = new DashboardQueryService(store);
        for (int i = 0; i < 3; i++) {
            seedSession("sess-" + i);
        }
    }

    private void seedSession(String sid) {
        store.saveSpans(List.of(
                new io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord(
                        "ss-" + sid, null, sid, -1, "SESSION", "session",
                        Instant.parse("2026-09-14T10:00:00Z"),
                        Instant.parse("2026-09-14T10:00:05Z"),
                        "OK", Map.of("agent.name", "ag"))));
    }

    @Test
    void oversizePageRequestIsClampedToCap() {
        DashboardQueryService.SessionPage page = service.listSessions(null, 1_000_000);
        // 三条数据全回（&lt;cap），页大小语义不被越界请求放大
        assertThat(page.items()).hasSize(3);
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void degeneratePageSizesNormalizeToOne() {
        assertThat(service.listSessions(null, 0).items()).hasSize(1);
        assertThat(service.listSessions(null, -7).items()).hasSize(1);
    }

    @Test
    void malformedCursorFailsWithReadableMessage() {
        // 原行为：裸 NumberFormatException 逃逸；现行为：可读 IAE
        assertThatThrownBy(() -> service.listSessions("not-a-number", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("游标格式非法");
        assertThatThrownBy(() -> service.listSessionsFiltered(
                null, null, null, null, null, "12x3", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("游标格式非法");
    }

    @Test
    void clampPreservesPagingSemantics() {
        // 翻页语义回归：小页 + 合法游标照常推进
        DashboardQueryService.SessionPage page1 = service.listSessions(null, 2);
        assertThat(page1.items()).hasSize(2);
        assertThat(page1.nextCursor()).isNotNull();
        DashboardQueryService.SessionPage page2 = service.listSessions(page1.nextCursor(), 2);
        assertThat(page2.items()).hasSize(1);
        assertThat(page2.nextCursor()).isNull();
    }

    @Test
    void filteredPathKeepsCapConstantAlignment() {
        DashboardQueryService.IndexedSessionPage page =
                service.listSessionsFiltered(null, null, null, null, null, null, 999_999);
        // 无索引 store → fromIndex=false 诚实降级（既有语义不变），页钳制同源
        assertThat(page.fromIndex()).isFalse();
        assertThat(page.items()).hasSize(3);
        assertThat(DashboardQueryService.MAX_PAGE_SIZE).isEqualTo(200);
    }
}
