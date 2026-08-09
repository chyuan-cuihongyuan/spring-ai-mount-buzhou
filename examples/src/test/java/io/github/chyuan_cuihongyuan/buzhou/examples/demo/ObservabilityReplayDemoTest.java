package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.TroubleshootingFixture;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityConfig;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityModule;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 簇 2 · 可观测回放（ticket 21 排障 demo）。
 *
 * <p>排障场景最看重「看清每轮推理依据」：Span 树还原 会话⊃轮次⊃模型/工具调用 的层级，
 * 注入快照按轮还原「模型当时实际看到的视图」，思维链与最终回复事件可追溯。三个测试覆盖：
 * <ul>
 *   <li>{@link #spanTreeFormsSessionTurnModelToolHierarchy}：并行工具调用的 Span 归属与层级树。</li>
 *   <li>{@link #injectionSnapshotReplaysWhatModelSaw}：按轮注入快照（预算分解含 tokens.total）。</li>
 *   <li>{@link #thinkingAndFinalReplyEventsCaptured}：思维链按 provider key 采集 + 最终回复事件。</li>
 * </ul>
 */
class ObservabilityReplayDemoTest {

    @Test
    void spanTreeFormsSessionTurnModelToolHierarchy() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        RuntimeConfig config = ObservabilityModule.configureSync(
                stores, ObservabilityConfig.testDefaults(), "demo-model");
        AgentRuntime runtime = Buzhou.runtime(model, stores, config,
                TroubleshootingFixture.fixedTool("query_logs", "rows"),
                TroubleshootingFixture.fixedTool("read_metric", "p99=120ms"));

        // 排障：模型并行查日志与指标，再给结论
        model.enqueue(AssistantMessage.builder().content("")
                .toolCalls(List.of(
                        new AssistantMessage.ToolCall("tc-1", "function", "query_logs", "{}"),
                        new AssistantMessage.ToolCall("tc-2", "function", "read_metric", "{}")))
                .build());
        model.enqueue(new AssistantMessage("定位到网关 p99 偏高"));

        AgentSession session = runtime.spawn("app", "agent", "sess-tree");
        session.chat("并行查日志与指标");
        session.close();

        List<SpanRecord> spans = stores.observabilityStore().spansOfSession("sess-tree");
        // 并行工具各自的 TOOL_CALL span，且 parent 是同一 TURN span（并发不串味）
        List<SpanRecord> tools = okSpansOfKind(spans, SpanKind.TOOL_CALL);
        assertThat(tools).extracting(SpanRecord::name)
                .containsExactlyInAnyOrder("tool:query_logs", "tool:read_metric");
        for (SpanRecord tc : tools) {
            assertThat(tc.sessionId()).isEqualTo("sess-tree");
            assertThat(parent(spans, tc).kind()).isEqualTo(SpanKind.TURN);
        }
        // TURN span 的 parent 是 SESSION span（完整嵌套树）
        for (SpanRecord turn : okSpansOfKind(spans, SpanKind.TURN)) {
            assertThat(parent(spans, turn).kind()).isEqualTo(SpanKind.SESSION);
        }
        // MODEL_CALL 也挂在 TURN 下
        for (SpanRecord mc : okSpansOfKind(spans, SpanKind.MODEL_CALL)) {
            assertThat(parent(spans, mc).kind()).isEqualTo(SpanKind.TURN);
        }
    }

    @Test
    void injectionSnapshotReplaysWhatModelSaw() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        RuntimeConfig config = ObservabilityModule.configureSync(
                stores, ObservabilityConfig.testDefaults(), "demo-model");
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        model.enqueue(new AssistantMessage("收到"));
        AgentSession session = runtime.spawn("app", "agent", "sess-snap");
        session.chat("看下现场");
        session.close();

        // 按轮还原「模型实际看到的注入视图」：压缩/spill 效果可解释
        assertThat(stores.observabilityStore().injectionSnapshot("sess-snap", 1)).isPresent()
                .get().satisfies(snap -> {
                    assertThat(snap.messageIds()).isNotEmpty();
                    assertThat(snap.budgetBreakdown()).containsKey("tokens.total");
                });
    }

    @Test
    void thinkingAndFinalReplyEventsCaptured() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        // deepseek → reasoningContent provider key 识别
        RuntimeConfig config = ObservabilityModule.configureSync(
                stores, ObservabilityConfig.testDefaults(), "deepseek");
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        model.enqueue(AssistantMessage.builder()
                .content("网关层超时")
                .properties(Map.of("reasoningContent", "先看日志再下结论"))
                .build());
        AgentSession session = runtime.spawn("app", "agent", "sess-think");
        session.chat("分析根因");
        session.close();

        List<EventRecord> events = stores.observabilityStore().eventsOfSession("sess-think");
        assertThat(events).anyMatch(e -> EventType.THINKING.equals(e.type())
                && "先看日志再下结论".equals(e.payload().get("content"))
                && "reasoningContent".equals(e.payload().get("provider.key")));
        assertThat(events).anyMatch(e -> EventType.FINAL_REPLY.equals(e.type()));
    }

    private static List<SpanRecord> okSpansOfKind(List<SpanRecord> spans, String kind) {
        return spans.stream().filter(s -> kind.equals(s.kind()) && "OK".equals(s.status())).toList();
    }

    private static SpanRecord parent(List<SpanRecord> spans, SpanRecord s) {
        return spans.stream().filter(x -> x.spanId().equals(s.parentSpanId()))
                .findFirst().orElseThrow(() -> new AssertionError("parent not found: " + s.parentSpanId()));
    }
}
