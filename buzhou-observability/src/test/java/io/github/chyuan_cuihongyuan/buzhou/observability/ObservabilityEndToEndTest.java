package io.github.chyuan_cuihongyuan.buzhou.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 可观测采集端到端（接缝 = AgentSession + 内存 SPI + ScriptedChatModel，对齐 spec 测试决策）。
 * 验收 checklist（ticket 11）四项在此覆盖。
 */
class ObservabilityEndToEndTest {

    @Test
    void parallelToolsProduceCorrectSpanTree() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        RuntimeConfig config = ObservabilityModule.configureSync(
                stores, ObservabilityConfig.testDefaults(), "test-model");
        AgentRuntime runtime = Buzhou.runtime(model, stores, config, fixedTool("tool_a", "A"), fixedTool("tool_b", "B"));

        // 第一轮：模型并行调两个工具，再回最终回复
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(
                        new AssistantMessage.ToolCall("tc-a", "function", "tool_a", "{}"),
                        new AssistantMessage.ToolCall("tc-b", "function", "tool_b", "{}")))
                .build());
        model.enqueue(new AssistantMessage("两个工具都完成了"));

        AgentSession session = runtime.spawn("app", "agent", "sess-tree");
        String reply = session.chat("并行调用两个工具");
        session.close();

        assertThat(reply).isEqualTo("两个工具都完成了");
        List<SpanRecord> spans = stores.observabilityStore().spansOfSession("sess-tree");
        // SESSION + TURN + 2×MODEL_CALL + 2×TOOL_CALL（RUNNING + OK 各一次 upsert，故每 span 出现两次）
        List<SpanRecord> toolCalls = spans.stream().filter(s -> SpanKind.TOOL_CALL.equals(s.kind())).toList();
        List<SpanRecord> turnSpans = spans.stream().filter(s -> SpanKind.TURN.equals(s.kind())).toList();
        List<SpanRecord> modelCalls = spans.stream().filter(s -> SpanKind.MODEL_CALL.equals(s.kind())).toList();
        // 两个工具名各出现（RUNNING + OK）
        assertThat(toolCalls).extracting(SpanRecord::name)
                .contains("tool:tool_a", "tool:tool_b");
        assertThat(toolCalls.stream().map(SpanRecord::name).distinct())
                .containsExactlyInAnyOrder("tool:tool_a", "tool:tool_b");
        assertThat(turnSpans).isNotEmpty();
        assertThat(modelCalls).isNotEmpty();

        // 并发归属：终态 TOOL_CALL 的 parent 必须都是 TURN span（spec 03 时序图定案），且属于同一会话
        List<SpanRecord> terminalToolCalls = filterTerminal(toolCalls);
        assertThat(terminalToolCalls).hasSize(2);
        for (SpanRecord tc : terminalToolCalls) {
            assertThat(tc.sessionId()).isEqualTo("sess-tree");
            SpanRecord parent = spans.stream().filter(s -> s.spanId().equals(tc.parentSpanId()))
                    .findFirst().orElseThrow(() -> new AssertionError(
                            "TOOL_CALL parent not found: " + tc.parentSpanId()));
            assertThat(parent.sessionId()).isEqualTo("sess-tree");
            assertThat(parent.kind()).isEqualTo(SpanKind.TURN);
        }
        // MODEL_CALL 的 parent 也必须是 TURN span（完整嵌套树：SESSION⊃TURN⊃MODEL_CALL/TOOL_CALL）
        for (SpanRecord mc : filterTerminal(modelCalls)) {
            SpanRecord parent = spans.stream().filter(s -> s.spanId().equals(mc.parentSpanId()))
                    .findFirst().orElseThrow(() -> new AssertionError(
                            "MODEL_CALL parent not found: " + mc.parentSpanId()));
            assertThat(parent.kind()).isEqualTo(SpanKind.TURN);
        }
        // TURN span 的 parent 是 SESSION span
        for (SpanRecord turn : filterTerminal(turnSpans)) {
            SpanRecord parent = spans.stream().filter(s -> s.spanId().equals(turn.parentSpanId()))
                    .findFirst().orElseThrow(() -> new AssertionError(
                            "TURN parent not found: " + turn.parentSpanId()));
            assertThat(parent.kind()).isEqualTo(SpanKind.SESSION);
        }
    }

    @Test
    void streamTurnCollectsFinalReplyAndClosesTurnSpan() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        RuntimeConfig config = ObservabilityModule.configureSync(
                stores, ObservabilityConfig.testDefaults(), "test-model");
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        model.enqueue(new AssistantMessage("流式回复"));

        AgentSession session = runtime.spawn("app", "agent", "sess-stream");
        StringBuilder received = new StringBuilder();
        session.stream("流式问一句")
                .doOnNext(r -> received.append(r.getResult().getOutput().getText()))
                .blockLast();
        session.close();

        assertThat(received.toString()).contains("流式回复");
        List<SpanRecord> spans = stores.observabilityStore().spansOfSession("sess-stream");
        // TURN span 必须关闭（OK 终态，不泄漏 RUNNING）
        List<SpanRecord> turns = spans.stream().filter(s -> SpanKind.TURN.equals(s.kind())).toList();
        assertThat(filterTerminal(turns)).isNotEmpty()
                .allMatch(t -> Boolean.TRUE.equals(t.attributes().get("turn.completed")));
        // 流式末次迭代产出 FINAL_REPLY（聚合正文）
        List<EventRecord> events = stores.observabilityStore().eventsOfSession("sess-stream");
        assertThat(events).anyMatch(e -> EventType.FINAL_REPLY.equals(e.type())
                && "流式回复".equals(e.payload().get("content")));
    }

    @Test
    void thinkingChainReasoningContentCapturedWithProviderKey() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        RuntimeConfig config = ObservabilityModule.configureSync(
                stores, ObservabilityConfig.testDefaults(), "deepseek");
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        model.enqueue(AssistantMessage.builder()
                .content("最终答案")
                .properties(Map.of("reasoningContent", "我先推理再回答"))
                .build());

        AgentSession session = runtime.spawn("app", "agent", "sess-thinking");
        session.chat("思考后回答");
        session.close();

        List<EventRecord> events = stores.observabilityStore().eventsOfSession("sess-thinking");
        assertThat(events).anyMatch(e -> EventType.THINKING.equals(e.type()));
        EventRecord thinking = events.stream().filter(e -> EventType.THINKING.equals(e.type()))
                .findFirst().orElseThrow();
        assertThat(thinking.payload()).containsEntry("content", "我先推理再回答");
        assertThat(thinking.payload()).containsEntry("provider.key", "reasoningContent");
    }

    @Test
    void officialOpenAiNoThinkingMarksProviderNotReturned() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        RuntimeConfig config = ObservabilityModule.configureSync(
                stores, ObservabilityConfig.testDefaults(), "gpt-5");
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        // 官方 OpenAI：无 reasoningContent
        model.enqueue(new AssistantMessage("直接回答"));

        AgentSession session = runtime.spawn("app", "agent", "sess-notthinking");
        session.chat("回答");
        session.close();

        List<EventRecord> events = stores.observabilityStore().eventsOfSession("sess-notthinking");
        assertThat(events).noneMatch(e -> EventType.THINKING.equals(e.type()));
        List<SpanRecord> modelCalls = stores.observabilityStore().spansOfSession("sess-notthinking").stream()
                .filter(s -> SpanKind.MODEL_CALL.equals(s.kind()))
                .toList();
        assertThat(modelCalls).anyMatch(s -> "PROVIDER_NOT_RETURNED".equals(s.attributes().get("thinking.available")));
    }

    @Test
    void injectionSnapshotCapturedPerTurn() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        RuntimeConfig config = ObservabilityModule.configureSync(
                stores, ObservabilityConfig.testDefaults(), "m");
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        model.enqueue(new AssistantMessage("好"));

        AgentSession session = runtime.spawn("app", "agent", "sess-snap");
        session.chat("你好");
        session.close();

        assertThat(stores.observabilityStore().injectionSnapshot("sess-snap", 1)).isPresent()
                .get().satisfies(snap -> {
                    assertThat(snap.messageIds()).isNotEmpty();
                    assertThat(snap.budgetBreakdown()).containsKey("tokens.total");
                });
    }

    @Test
    void closeFlushesRemainingEvents() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        // 用异步管线 + 默认配置验证 close flush 不丢
        RuntimeConfig config = ObservabilityModule.configure(
                stores, ObservabilityConfig.testDefaults(), null, "m");
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        model.enqueue(new AssistantMessage("结束"));

        AgentSession session = runtime.spawn("app", "agent", "sess-flush");
        session.chat("最后一轮");
        session.close();

        List<EventRecord> events = stores.observabilityStore().eventsOfSession("sess-flush");
        assertThat(events).anyMatch(e -> EventType.FINAL_REPLY.equals(e.type()));
    }

    private List<SpanRecord> filterTerminal(List<SpanRecord> toolCalls) {
        // 取每个工具的终态（OK）记录；RUNNING 与 OK 因 upsert 都在
        return toolCalls.stream().filter(s -> "OK".equals(s.status())).toList();
    }

    static ToolCallback fixedTool(String name, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    static class ScriptedChatModel implements ChatModel {
        final Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();

        void enqueue(AssistantMessage message) {
            script.add(new ChatResponse(List.of(new Generation(message))));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            ChatResponse next = script.poll();
            if (next == null) {
                next = new ChatResponse(List.of(new Generation(new AssistantMessage("default reply"))));
            }
            return next;
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }
}
