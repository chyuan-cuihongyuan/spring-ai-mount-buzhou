package io.github.chyuan_cuihongyuan.buzhou.observability.advisor;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityConfig;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.BaseSpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PendingEvent;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PendingItem;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PendingSpan;
import io.github.chyuan_cuihongyuan.buzhou.observability.thinking.ThinkingChainExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ObservabilityAdvisor 流式路径分支补测（K 会话 R13 / spec 1212 / T1833——R6 逐方法分支数据
 * 精定制导：accumulateStreamChunk / markFirstTokenIfNeeded / recordTpotIfNeeded /
 * recordStreamOutcome / resolveTurnParent）。harness：StreamAdvisorChain stub +
 * ChatClientRequest(Builder) + ChatResponse(Generation/AssistantMessage/DefaultUsage)。
 * 事件与 span 经 RecordingBase.doEnqueue 捕获（PendingSpan/PendingEvent 拆箱断言）。
 */
class ObservabilityAdvisorStreamTest {

    static final class RecordingBase extends BaseSpanRecorder {
        final List<PendingItem> items = new ArrayList<>();

        RecordingBase() {
            super(new MicrometerDualWriter(), false);
        }

        @Override
        protected void doEnqueue(PendingItem item) {
            items.add(item);
        }

        @Override
        public void flush() {
        }
    }

    private RecordingBase recorder;
    private ObservabilitySessionState state;
    private ObservabilityAdvisor advisor;

    @BeforeEach
    void setUp() {
        recorder = new RecordingBase();
        state = new ObservabilitySessionState(recorder, null, "sess-1", "agent", "app", "gpt");
        advisor = new ObservabilityAdvisor(recorder,
                new ObservabilityConfig(true, 200, Duration.ofSeconds(1), Duration.ofSeconds(5), 10000,
                        true, null, 32768, false, true, false, "openai"),
                new ThinkingChainExtractor(null, 100),
                estimator(),
                state, "gpt");
        state.onOpen();
        state.onTurnStart(1, "hi");
    }

    private static io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator estimator() {
        return new io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator() {
            @Override
            public int estimate(String text) {
                return text == null ? 0 : text.length();
            }

            @Override
            public int estimateMessages(List<org.springframework.ai.chat.messages.Message> messages) {
                return 0;
            }

            @Override
            public String name() {
                return "test";
            }
        };
    }

    private ChatClientRequest request() {
        return new ChatClientRequest(new Prompt(List.of(new UserMessage("hi"))), new HashMap<>());
    }

    private static ChatResponse textChunk(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static ChatResponse textChunkWithFinish(String text, String finishReason, Usage usage) {
        Generation gen = new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(finishReason).build());
        ChatResponseMetadata metadata = usage == null ? null
                : ChatResponseMetadata.builder().usage(usage).build();
        return new ChatResponse(List.of(gen), metadata);
    }

    private static ChatResponse thinkingChunk(String thinking) {
        AssistantMessage msg = AssistantMessage.builder().content("")
                .properties(Map.of("thinking", thinking)).build();
        return new ChatResponse(List.of(new Generation(msg)));
    }

    private static ChatResponse toolCallChunk() {
        AssistantMessage msg = AssistantMessage.builder().content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("id1", "function", "read_file", "{}")))
                .build();
        return new ChatResponse(List.of(new Generation(msg)));
    }

    private static ChatResponse usageChunk(int prompt, int completion) {
        Usage usage = new DefaultUsage(prompt, completion, prompt + completion, new Object());
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        return new ChatResponse(List.of(), metadata);
    }

    private StreamAdvisorChain chain(Flux<ChatClientResponse> flux) {
        return new StreamAdvisorChain() {
            @Override
            public Flux<ChatClientResponse> nextStream(ChatClientRequest req) {
                return flux;
            }

            @Override
            public List<org.springframework.ai.chat.client.advisor.api.StreamAdvisor> getStreamAdvisors() {
                return List.of();
            }

            @Override
            public StreamAdvisorChain copy(org.springframework.ai.chat.client.advisor.api.StreamAdvisor a) {
                return this;
            }
        };
    }

    private Flux<ChatClientResponse> responses(List<ChatResponse> chunks) {
        return Flux.fromIterable(chunks).map(c -> new ChatClientResponse(c, new HashMap<>()));
    }

    /** 驱动流式订阅至完成，返回收到的 ChatClientResponse 列表。 */
    private List<ChatClientResponse> advise(List<ChatResponse> chunks) {
        return advisor.adviseStream(request(), chain(responses(chunks)))
                .collectList()
                .block();
    }

    /** 驱动流式订阅并在首个元素后取消（触发 doOnCancel）。 */
    private void adviseAndCancel(List<ChatResponse> chunks) {
        advisor.adviseStream(request(), chain(responses(chunks))).take(1).blockLast();
    }

    private List<SpanRecord> spanRecords() {
        return recorder.items.stream()
                .filter(PendingSpan.class::isInstance)
                .map(i -> ((PendingSpan) i).record())
                .toList();
    }

    private SpanRecord lastSpan() {
        List<SpanRecord> spans = spanRecords();
        return spans.get(spans.size() - 1);
    }

    private List<String> eventTypes() {
        return recorder.items.stream()
                .filter(PendingEvent.class::isInstance)
                .map(i -> ((PendingEvent) i).record().type())
                .toList();
    }

    @Test
    void streamHappyPathRecordsTtftThinkingReplyUsageAndTpot() {
        advise(List.of(
                thinkingChunk("thought-1"),
                textChunkWithFinish("answer", "stop", new DefaultUsage(10, 3, 13, new Object()))));

        List<SpanRecord> spans = spanRecords();
        // SESSION（onOpen） + TURN（onTurnStart） + MODEL_CALL RUNNING + MODEL_CALL 终态
        assertThat(spans).hasSize(4);
        SpanRecord modelCall = spans.get(3);
        assertThat(modelCall.status()).isEqualTo(SpanStatus.OK);
        assertThat(modelCall.attributes().get("ttft.ms")).isNotNull();
        assertThat(modelCall.attributes().get("tpot.ms")).isNotNull();
        assertThat(modelCall.attributes().get("usage.prompt_tokens")).isEqualTo(10);
        assertThat(modelCall.attributes().get("usage.completion_tokens")).isEqualTo(3);
        assertThat(modelCall.attributes().get("finish_reason")).isEqualTo("stop");
        assertThat(eventTypes()).contains("STREAM_FIRST_TOKEN", "THINKING", "FINAL_REPLY");
    }

    @Test
    void usageOnlyStreamRecordsUsageWithoutFirstToken() {
        advise(List.of(usageChunk(10, 3)));

        SpanRecord modelCall = lastSpan();
        assertThat(modelCall.status()).isEqualTo(SpanStatus.OK);
        assertThat(modelCall.attributes().get("usage.prompt_tokens")).isEqualTo(10);
        // usage-only 无内容信号：无 TTFT 属性（口径：首内容到达时刻）
        assertThat(modelCall.attributes().get("ttft.ms")).isNull();
        assertThat(eventTypes()).doesNotContain("STREAM_FIRST_TOKEN");
    }

    @Test
    void sawToolCallsSuppressesFinalReplyEvent() {
        advise(List.of(
                toolCallChunk(),
                textChunkWithFinish("", "tool_calls", null),
                usageChunk(5, 2)));

        assertThat(eventTypes()).doesNotContain("FINAL_REPLY");
        assertThat(lastSpan().attributes().get("finish_reason")).isEqualTo("tool_calls");
    }

    @Test
    void noCompletionUsageSkipsTpotButRecordsTokens() {
        // usage.completionTokens = null：usage 记 prompt 桶、TPOT 跳过
        advise(List.of(textChunkWithFinish("answer", "stop",
                new DefaultUsage(10, null, 10, new Object()))));

        SpanRecord modelCall = lastSpan();
        assertThat(modelCall.attributes().get("usage.prompt_tokens")).isEqualTo(10);
        assertThat(modelCall.attributes().get("tpot.ms")).isNull();
    }

    @Test
    void streamErrorMarksSpanErrorAndCloses() {
        RuntimeException boom = new RuntimeException("stream boom");

        assertThatThrownBy(() ->
                        advisor.adviseStream(request(), chain(Flux.error(boom))).collectList().block())
                .isSameAs(boom);

        assertThat(lastSpan().status()).isEqualTo(SpanStatus.ERROR);
    }

    @Test
    void cancelMarksSpanCancelled() {
        adviseAndCancel(List.of(textChunk("part-1"), textChunk("part-2"), textChunk("part-3")));

        assertThat(lastSpan().status()).isEqualTo(SpanStatus.CANCELLED);
    }

    @Test
    void metadataNullAndResultNullChunksAreDefensive() {
        // metadata=null（usage 不捕获）与 result=null（空 generations 早退）均不炸
        advise(List.of(
                new ChatResponse(List.of(), null),
                textChunkWithFinish("answer", "stop", null)));

        List<SpanRecord> spans = spanRecords();
        SpanRecord modelCall = spans.get(spans.size() - 1);
        assertThat(modelCall.status()).isEqualTo(SpanStatus.OK);
        // metadata=null 被 ChatResponse 归一为 NULL 常量（usage 0/0/0）——usage-null 分支防御性不可达（R17 入档）
        assertThat(modelCall.attributes().get("usage.prompt_tokens")).isEqualTo(0);
        assertThat(modelCall.attributes().get("finish_reason")).isEqualTo("stop");
    }

    @Test
    void blankFinishReasonIsNotRecorded() {
        advise(List.of(textChunkWithFinish("answer", "  ", null)));

        assertThat(lastSpan().attributes()).doesNotContainKey("finish_reason");
    }

    @Test
    void blankThinkingChunkProducesNoThinkingEvent() {
        advise(List.of(
                thinkingChunk("   "),
                textChunkWithFinish("answer", "stop", null)));

        // 空串思维链被 stringOf 过滤：无 THINKING 事件（模型无思维链口径）
        assertThat(eventTypes()).doesNotContain("THINKING");
        assertThat(lastSpan().attributes().get("thinking.available")).isNull();
    }

    @Test
    void ttftFiresExactlyOnceAcrossMultipleContentChunks() {
        advise(List.of(
                thinkingChunk("thought-1"),
                thinkingChunk("thought-2"),
                textChunkWithFinish("answer", "stop", null)));

        assertThat(eventTypes().stream().filter(t -> t.equals("STREAM_FIRST_TOKEN")).count()).isEqualTo(1);
    }

    @Test
    void omittedOnlyChunkProducesNoThinkingEventAndNoContentSignal() {
        advise(List.of(
                new ChatResponse(List.of(new Generation(AssistantMessage.builder().content("")
                        .properties(Map.of(ThinkingChainExtractor.ATTR_OMITTED, "true")).build()))),
                textChunkWithFinish("answer", "stop", null))));

        assertThat(eventTypes()).doesNotContain("THINKING", "STREAM_FIRST_TOKEN");
        assertThat(lastSpan().attributes().get("thinking.available")).isNull();
    }

    @Test
    void emptyFluxCompletesAndClosesSpanGracefully() {
        advise(List.of());

        assertThat(lastSpan().status()).isEqualTo(SpanStatus.OK);
        assertThat(eventTypes()).doesNotContain("STREAM_FIRST_TOKEN");
    }

    @Test
    void resolveTurnParentFallsBackToSessionSpanThenNull() {
        // 有 session（setUp 已 onOpen）→ MODEL_CALL parent 非空
        advise(List.of(textChunkWithFinish("a", "stop", null)));
        assertThat(spanRecords().get(spanRecords().size() - 1).parentSpanId()).isNotNull();

        // 无 session、无 turn：parent = null（兜底分支）
        ObservabilitySessionState bare = new ObservabilitySessionState(recorder, null, "sess-9", "a", "b", "m");
        ObservabilityAdvisor bareAdvisor = new ObservabilityAdvisor(recorder,
                new ObservabilityConfig(true, 200, Duration.ofSeconds(1), Duration.ofSeconds(5), 10000,
                        true, null, 32768, false, true, false, "openai"),
                new ThinkingChainExtractor(null, 100),
                estimator(),
                bare, "gpt");
        bareAdvisor.adviseStream(request(), chain(responses(List.of(
                textChunkWithFinish("a", "stop", null))))).collectList().block();
        assertThat(spanRecords().get(spanRecords().size() - 1).parentSpanId()).isNull();
    }
}