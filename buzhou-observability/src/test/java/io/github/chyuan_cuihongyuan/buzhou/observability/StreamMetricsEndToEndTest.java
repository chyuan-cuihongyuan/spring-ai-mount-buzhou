package io.github.chyuan_cuihongyuan.buzhou.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 46 §A / T170 / impl-139：流式 TTFT/TPOT 指标端到端。
 * 口径：首内容信号 = 正文/思维链累计器空→非空或工具调用 delta 信号；usage-only 空块不触发。
 */
class StreamMetricsEndToEndTest {

    private final List<String> timers = new CopyOnWriteArrayList<>();

    @AfterEach
    void reset() {
        BuzhouMetricsHolder.reset();
    }

    private void installCapturingMetrics() {
        BuzhouMetricsHolder.install(new BuzhouMetrics() {
            @Override
            public void counter(String name, long delta, String... tagKeyValue) {
            }

            @Override
            public void timer(String name, Duration duration, String... tagKeyValue) {
                timers.add(name);
            }
        });
    }

    /** testDefaults 的 micrometer 开启版（否则 recorder.meters() 为 NOOP，timer 断言全空）。 */
    private static ObservabilityConfig testConfigWithMeters() {
        return new ObservabilityConfig(true, 1, Duration.ofMillis(10), Duration.ofSeconds(5),
                10000, true, List.of(), 32768, true, true, true, null);
    }

    /** 空块在前（延迟 150ms）+ 两个内容块：TTFT 应计首内容块时刻而非首信号时刻。 */
    @Test
    void firstContentSignalRecordsTtftAndTpot() {
        installCapturingMetrics();
        Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();
        ScriptedStreamingModel model = new ScriptedStreamingModel(Flux.concat(
                // 首信号：usage-only 空块（正文空串不进累计器），延迟 150ms 到达
                Flux.just(emptyUsageChunk()).delayElements(Duration.ofMillis(150)),
                // 首内容块
                Flux.just(textChunk("你")).delayElements(Duration.ofMillis(100)),
                // 末块：内容 + usage(completion=2)，再延迟 100ms → TPOT = (总−TTFT)/1 ≈ 100ms 可记
                Flux.just(textChunkWithUsage("好", 3, 2)).delayElements(Duration.ofMillis(100))));
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                ObservabilityModule.configureSync(stores, testConfigWithMeters(), "test-model"));

        AgentSession session = runtime.spawn("app", "agent", "sess-ttft");
        session.stream("流式问一句").blockLast();
        session.close();

        List<EventRecord> events = stores.observabilityStore().eventsOfSession("sess-ttft");
        List<EventRecord> firstToken = events.stream()
                .filter(e -> EventType.STREAM_FIRST_TOKEN.equals(e.type())).toList();
        assertThat(firstToken).hasSize(1);
        long ttftMs = ((Number) firstToken.get(0).payload().get("ttft.ms")).longValue();
        // 首内容块在订阅后约 250ms 到达（空块 150ms + 100ms）；TTFT ≥ 200ms 证明空块未触发
        assertThat(ttftMs).isBetween(200L, 5000L);

        // MODEL_CALL span 属性 ttft.ms / tpot.ms（completion=2 → TPOT = (总−TTFT)/1 可记）
        List<SpanRecord> modelCalls = stores.observabilityStore().spansOfSession("sess-ttft").stream()
                .filter(s -> SpanKind.MODEL_CALL.equals(s.kind()) && "OK".equals(s.status())).toList();
        assertThat(modelCalls).isNotEmpty();
        assertThat(modelCalls).allMatch(s -> ((Number) s.attributes().get("ttft.ms")).longValue() >= 200);
        assertThat(modelCalls).allMatch(s -> s.attributes().containsKey("tpot.ms"));

        assertThat(timers).contains("buzhou.model.ttft", "buzhou.model.tpot");
    }

    /** 全程无内容（仅空块）→ 无 TTFT 事件、无 timer、无 tpot 属性。 */
    @Test
    void noContentStreamRecordsNothing() {
        installCapturingMetrics();
        ScriptedStreamingModel model = new ScriptedStreamingModel(
                Flux.just(emptyUsageChunk(), emptyUsageChunk()));
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                ObservabilityModule.configureSync(stores, testConfigWithMeters(), "m"));

        AgentSession session = runtime.spawn("app", "agent", "sess-nottft");
        session.stream("问").blockLast();
        session.close();

        List<EventRecord> events = stores.observabilityStore().eventsOfSession("sess-nottft");
        assertThat(events).noneMatch(e -> EventType.STREAM_FIRST_TOKEN.equals(e.type()));
        assertThat(timers).doesNotContain("buzhou.model.ttft", "buzhou.model.tpot");
        List<SpanRecord> modelCalls = stores.observabilityStore().spansOfSession("sess-nottft").stream()
                .filter(s -> SpanKind.MODEL_CALL.equals(s.kind()) && "OK".equals(s.status())).toList();
        assertThat(modelCalls).allMatch(s -> !s.attributes().containsKey("tpot.ms"));
    }

    /** 单内容块（completion=1）→ TTFT 有、TPOT 无（单 token 无均摊意义）。 */
    @Test
    void singleCompletionTokenSkipsTpot() {
        installCapturingMetrics();
        ScriptedStreamingModel model = new ScriptedStreamingModel(
                Flux.just(textChunkWithUsage("只有一字", 3, 1)));
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                ObservabilityModule.configureSync(stores, testConfigWithMeters(), "m"));

        AgentSession session = runtime.spawn("app", "agent", "sess-tpot1");
        session.stream("问").blockLast();
        session.close();

        assertThat(stores.observabilityStore().eventsOfSession("sess-tpot1"))
                .anyMatch(e -> EventType.STREAM_FIRST_TOKEN.equals(e.type()));
        assertThat(timers).contains("buzhou.model.ttft").doesNotContain("buzhou.model.tpot");
    }

    private static ChatResponse emptyUsageChunk() {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(""))),
                ChatResponseMetadata.builder().usage(new DefaultUsage(10, 0)).build());
    }

    private static ChatResponse textChunk(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static ChatResponse textChunkWithUsage(String text, int promptTokens, int completionTokens) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))),
                ChatResponseMetadata.builder().usage(new DefaultUsage(promptTokens, completionTokens)).build());
    }

    /** 固定流脚本模型（stream() 返回构造给定 Flux；call() 兜底空回复）。 */
    static final class ScriptedStreamingModel implements ChatModel {
        private final Flux<ChatResponse> streamScript;
        private final Queue<ChatResponse> fallback = new ConcurrentLinkedQueue<>();

        ScriptedStreamingModel(Flux<ChatResponse> streamScript) {
            this.streamScript = streamScript;
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            ChatResponse next = fallback.poll();
            return next != null ? next
                    : new ChatResponse(List.of(new Generation(new AssistantMessage("default"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return streamScript;
        }
    }
}
