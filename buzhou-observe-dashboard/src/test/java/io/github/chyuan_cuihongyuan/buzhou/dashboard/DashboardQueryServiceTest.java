package io.github.chyuan_cuihongyuan.buzhou.dashboard;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dashboard 查询门面（ticket 17 验收：会话回放完整 Span/Event 树、按轮次注入快照、
 * token/耗时统计与 Span 属性一致）。
 */
class DashboardQueryServiceTest {

    private static final Instant T0 = Instant.parse("2026-08-08T10:00:00Z");

    private io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore store;
    private DashboardQueryService service;

    @BeforeEach
    void setUp() {
        store = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores().observabilityStore();
        service = new DashboardQueryService(store);
        seedSession("sess-1");
    }

    /** 典型一轮：SESSION ⊃ TURN ⊃ MODEL_CALL + TOOL_CALL，事件带思维链/出入参/最终回复。 */
    private void seedSession(String sid) {
        store.saveSpans(List.of(
                span("ss-" + sid, null, sid, -1, "SESSION", "session", 0, 5000,
                        "OK", Map.of("agent.name", "demo-agent")),
                span("t1-" + sid, "ss-" + sid, sid, 1, "TURN", "turn-1", 0, 3000,
                        "OK", Map.of("turn.completed", true)),
                span("m1-" + sid, "t1-" + sid, sid, 1, "MODEL_CALL", "model-call", 100, 900,
                        "OK", Map.of("model.name", "gpt-test", "iteration", 1,
                                "usage.prompt_tokens", 120, "usage.completion_tokens", 30)),
                span("tc1-" + sid, "t1-" + sid, sid, 1, "TOOL_CALL", "tool:read_file", 1000, 1600,
                        "OK", Map.of("tool.name", "read_file", "tool.parallel.index", 0)),
                span("m2-" + sid, "t1-" + sid, sid, 1, "MODEL_CALL", "model-call", 1700, 2900,
                        "OK", Map.of("model.name", "gpt-test", "iteration", 2,
                                "usage.prompt_tokens", 200, "usage.completion_tokens", 80,
                                "usage.reasoning_tokens", 40)),
                span("t2-" + sid, "ss-" + sid, sid, 2, "TURN", "turn-2", 3000, 5000,
                        "OK", Map.of("turn.completed", true)),
                span("m3-" + sid, "t2-" + sid, sid, 2, "MODEL_CALL", "model-call", 3100, 4900,
                        "ERROR", Map.of("model.name", "gpt-test", "iteration", 1,
                                "usage.prompt_tokens", 90, "usage.completion_tokens", 0))));
        store.saveEvents(List.of(
                event("e-think", "m1-" + sid, sid, "THINKING", Map.of("content", "先读文件")),
                event("e-ti", "tc1-" + sid, sid, "TOOL_INPUT",
                        Map.of("tool.name", "read_file", "arguments", "{\"path\":\"a.txt\"}")),
                event("e-to", "tc1-" + sid, sid, "TOOL_OUTPUT",
                        Map.of("tool.name", "read_file", "result", "文件内容")),
                event("e-final", "m2-" + sid, sid, "FINAL_REPLY", Map.of("content", "已读取"))));
        store.saveInjectionSnapshot(new InjectionSnapshot(sid, 1, List.of("m1"),
                List.of(), Map.of("historyBudget", 4096), "v1", T0));
    }

    private static SpanRecord span(String id, String parent, String sid, int turnSeq,
                                   String kind, String name, long startOffsetMs, long endOffsetMs,
                                   String status, Map<String, Object> attributes) {
        return new SpanRecord(id, parent, sid, turnSeq, kind, name,
                T0.plusMillis(startOffsetMs), T0.plusMillis(endOffsetMs), status, attributes);
    }

    private static EventRecord event(String id, String spanId, String sid, String type,
                                     Map<String, Object> payload) {
        return new EventRecord(id, spanId, sid, type, T0, payload);
    }

    @Test
    void replayGroupsSpansAndEventsByTurn() {
        DashboardQueryService.ReplayView replay = service.replay("sess-1");

        assertThat(replay.turns()).hasSize(2);
        DashboardQueryService.TurnReplay turn1 = replay.turns().get(0);
        assertThat(turn1.turnSeq()).isEqualTo(1);
        assertThat(turn1.turnSpan().kind()).isEqualTo("TURN");
        assertThat(turn1.spans()).extracting(SpanRecord::spanId)
                .containsExactlyInAnyOrder("t1-sess-1", "m1-sess-1", "tc1-sess-1", "m2-sess-1");
        assertThat(turn1.events()).extracting(EventRecord::type)
                .containsExactlyInAnyOrder("THINKING", "TOOL_INPUT", "TOOL_OUTPUT", "FINAL_REPLY");
        assertThat(turn1.hasSnapshot()).isTrue();
        assertThat(replay.turns().get(1).hasSnapshot()).isFalse();
        assertThat(replay.unboundEvents()).isEmpty();
    }

    @Test
    void spansTreeViewNestsByParent() {
        Object flat = service.spans("sess-1", "flat");
        assertThat((List<?>) flat).hasSize(7);

        @SuppressWarnings("unchecked")
        List<DashboardQueryService.SpanNode> roots =
                (List<DashboardQueryService.SpanNode>) service.spans("sess-1", "tree");
        assertThat(roots).hasSize(1);
        DashboardQueryService.SpanNode session = roots.get(0);
        assertThat(session.span().kind()).isEqualTo("SESSION");
        assertThat(session.children()).extracting(n -> n.span().spanId())
                .containsExactlyInAnyOrder("t1-sess-1", "t2-sess-1");
        DashboardQueryService.SpanNode turn1 = session.children().stream()
                .filter(n -> n.span().spanId().equals("t1-sess-1")).findFirst().orElseThrow();
        assertThat(turn1.children()).extracting(n -> n.span().spanId())
                .containsExactlyInAnyOrder("m1-sess-1", "tc1-sess-1", "m2-sess-1");
    }

    @Test
    void statsConsistentWithSpanAttributes() {
        DashboardQueryService.SessionStats stats = service.stats("sess-1");

        // 总量 = MODEL_CALL usage 属性求和（120+200+90 / 30+80+0）
        assertThat(stats.totalPromptTokens()).isEqualTo(410);
        assertThat(stats.totalCompletionTokens()).isEqualTo(110);
        // 会话耗时 = 最早 start 到最晚 end（0 → 5000ms）
        assertThat(stats.totalDurationMs()).isEqualTo(5000);

        assertThat(stats.perTurn()).hasSize(2);
        DashboardQueryService.TurnStats t1 = stats.perTurn().get(0);
        assertThat(t1.turnSeq()).isEqualTo(1);
        assertThat(t1.promptTokens()).isEqualTo(320);
        assertThat(t1.completionTokens()).isEqualTo(110);
        assertThat(t1.durationMs()).isEqualTo(3000);
        assertThat(t1.iterations()).isEqualTo(2);

        assertThat(stats.perModel()).singleElement().satisfies(m -> {
            assertThat(m.model()).isEqualTo("gpt-test");
            assertThat(m.calls()).isEqualTo(3);
            assertThat(m.reasoningTokens()).isEqualTo(40);
            assertThat(m.totalDurationMs()).isEqualTo(800 + 1200 + 1800);
        });
        assertThat(stats.perTool()).singleElement().satisfies(t -> {
            assertThat(t.tool()).isEqualTo("read_file");
            assertThat(t.calls()).isEqualTo(1);
            assertThat(t.errors()).isZero();
            assertThat(t.totalDurationMs()).isEqualTo(600);
        });
    }

    @Test
    void sessionsPageCarriesNextCursor() {
        service.replay("sess-1"); // 触发无写入，仅保证 sess-1 存在
        seedSecondStore();
        DashboardQueryService.SessionPage page1 = service.listSessions(null, 1);
        assertThat(page1.items()).hasSize(1);
        assertThat(page1.nextCursor()).isEqualTo("1");
        DashboardQueryService.SessionPage page2 = service.listSessions(page1.nextCursor(), 5);
        assertThat(page2.items()).hasSize(1);
        assertThat(page2.nextCursor()).isNull();
    }

    private void seedSecondStore() {
        store.saveSpans(List.of(span("t1-sess-2", null, "sess-2", 1, "TURN", "turn-1",
                6000, 7000, "OK", Map.of())));
    }

    @Test
    void snapshotFetchedByTurn() {
        assertThat(service.snapshot("sess-1", 1)).isPresent()
                .get().satisfies(s -> assertThat(s.budgetBreakdown())
                        .containsEntry("historyBudget", 4096));
        assertThat(service.snapshot("sess-1", 2)).isEmpty();
    }

    @Test
    void eventsOfSpanReturnsAttachedEvents() {
        assertThat(service.eventsOfSpan("tc1-sess-1"))
                .extracting(EventRecord::type)
                .containsExactlyInAnyOrder("TOOL_INPUT", "TOOL_OUTPUT");
    }
}
