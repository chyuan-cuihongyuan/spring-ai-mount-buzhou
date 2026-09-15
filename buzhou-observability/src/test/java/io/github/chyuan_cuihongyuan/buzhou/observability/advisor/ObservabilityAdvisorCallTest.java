package io.github.chyuan_cuihongyuan.buzhou.observability.advisor;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityConfig;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.BaseSpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PendingItem;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PendingSpan;
import io.github.chyuan_cuihongyuan.buzhou.observability.thinking.ThinkingChainExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ObservabilityAdvisor 非流式路径分支补测（K 会话 R16 / spec 1214 / T1839——R13 复测逐分支
 * 数据精定制导，recordModelCallOutcome 22 missed 集中区）：adviseCall 全链（open→nextCall→
 * outcome→close）、usage 三态、thinking 三态（PROVIDER_NOT_RETURNED 启发式/显式 provider/NO）、
 * toolCalls 与 blank 文本抑制 FINAL_REPLY、null response 防御、chain 异常标 ERROR。
 * 流式路径由 ObservabilityAdvisorStreamTest 覆盖。
 */
class ObservabilityAdvisorCallTest {

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

    private ObservabilityAdvisor advisor(String modelName, String modelProvider,
                                         boolean snapshotCapture, int thinkingMaxChars) {
        recorder = new RecordingBase();
        state = new ObservabilitySessionState(recorder, null, "sess-1", "agent", "app", modelName);
        state.onOpen();
        state.onTurnStart(1, "hi");
        return new ObservabilityAdvisor(recorder,
                new ObservabilityConfig(true, 200, Duration.ofSeconds(1), Duration.ofSeconds(5), 10000,
                        true, null, thinkingMaxChars, false, snapshotCapture, false, modelProvider),
                new ThinkingChainExtractor(null, thinkingMaxChars),
                estimator(),
                state, modelName);
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

    private static ChatResponse chatResponse(AssistantMessage message, Usage usage, String finishReason) {
        Generation gen = new Generation(message,
                ChatGenerationMetadata.builder().finishReason(finishReason).build());
        ChatResponseMetadata metadata = usage == null ? null
                : ChatResponseMetadata.builder().usage(usage).build();
        return new ChatResponse(List.of(gen), metadata);
    }

    private CallAdvisorChain chainReturning(ChatClientResponse response) {
        return new CallAdvisorChain() {
            @Override
            public ChatClientResponse nextCall(ChatClientRequest req) {
                return response;
            }

            @Override
            public List<org.springframework.ai.chat.client.advisor.api.CallAdvisor> getCallAdvisors() {
                return List.of();
            }

            @Override
            public CallAdvisorChain copy(org.springframework.ai.chat.client.advisor.api.CallAdvisor a) {
                return this;
            }
        };
    }

    private List<SpanRecord> spanRecords() {
        return recorder.items.stream()
                .filter(PendingSpan.class::isInstance)
                .map(i -> ((PendingSpan) i).record())
                .toList();
    }

    private List<String> eventPayloads() {
        return recorder.items.stream()
                .filter(i -> i instanceof io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PendingEvent)
                .map(i -> ((io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PendingEvent) i)
                        .record().toString())
                .toList();
    }

    private List<String> eventTypes() {
        return recorder.items.stream()
                .filter(i -> i instanceof io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PendingEvent)
                .map(i -> ((io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PendingEvent) i)
                        .record().type())
                .toList();
    }

    private SpanRecord lastSpan() {
        List<SpanRecord> spans = spanRecords();
        return spans.get(spans.size() - 1);
    }

    @Test
    void adviseCallHappyPathRecordsThinkingReplyUsageAndAccumulatesTurn() {
        ObservabilityAdvisor advisor = advisor("gpt-4o", null, true, 32768);
        AssistantMessage msg = AssistantMessage.builder().content("最终答复")
                .properties(Map.of("reasoningContent", "思考过程", "reasoning_signature", "sig-1"))
                .build();

        ChatClientResponse out = advisor.adviseCall(request(),
                chainReturning(new ChatClientResponse(chatResponse(msg,
                        new DefaultUsage(10, 3, 13, new Object()), "stop"), new HashMap<>())));

        assertThat(out).isNotNull();
        SpanRecord modelCall = lastSpan();
        assertThat(modelCall.status()).isEqualTo(SpanStatus.OK);
        assertThat(modelCall.attributes().get("usage.prompt_tokens")).isEqualTo(10);
        assertThat(modelCall.attributes().get("finish_reason")).isEqualTo("stop");
        assertThat(modelCall.attributes().get("thinking.available")).isEqualTo("YES");
        // hooks instanceof ObservabilitySessionState：usage 聚合到会话状态（onTurnEnd 读）
        state.onTurnEnd(1, "最终答复");
        SpanRecord turnFinal = spanRecords().get(spanRecords().size() - 1);
        assertThat(turnFinal.attributes().get("usage.prompt_tokens")).isEqualTo(10);
    }

    @Test
    void nullResponseClosesModelCallWithNoEvents() {
        ObservabilityAdvisor advisor = advisor("gpt-4o", null, true, 32768);

        ChatClientResponse out = advisor.adviseCall(request(), chainReturning(null));

        assertThat(out).isNull();
        assertThat(lastSpan().status()).isEqualTo(SpanStatus.OK);
        assertThat(eventTypes()).isEmpty();
    }

    @Test
    void nullChatResponseClosesModelCallWithNoEvents() {
        ObservabilityAdvisor advisor = advisor("gpt-4o", null, true, 32768);

        advisor.adviseCall(request(), chainReturning(new ChatClientResponse(null, new HashMap<>())));

        assertThat(lastSpan().status()).isEqualTo(SpanStatus.OK);
        assertThat(eventTypes()).isEmpty();
    }

    @Test
    void usageWithoutCompletionRecordsPromptOnly() {
        ObservabilityAdvisor advisor = advisor("gpt-4o", null, true, 32768);
        AssistantMessage msg = new AssistantMessage("答复");

        advisor.adviseCall(request(), chainReturning(new ChatClientResponse(chatResponse(msg,
                new DefaultUsage(10, null, 10, new Object()), "stop"), new HashMap<>())));

        SpanRecord modelCall = lastSpan();
        assertThat(modelCall.attributes().get("usage.prompt_tokens")).isEqualTo(10);
        // DefaultUsage 归一 null completion → 0：完成侧指标仍以 0 记录（口径不缺位）
        assertThat(modelCall.attributes().get("usage.completion_tokens")).isEqualTo(0);
    }

    @Test
    void noThinkingOnGptModelMarksProviderNotReturned() {
        ObservabilityAdvisor advisor = advisor("gpt-4o", null, true, 32768);
        AssistantMessage msg = new AssistantMessage("纯文本答复");

        advisor.adviseCall(request(), chainReturning(new ChatClientResponse(
                chatResponse(msg, null, "stop"), new HashMap<>())));

        assertThat(lastSpan().attributes().get("thinking.available")).isEqualTo("PROVIDER_NOT_RETURNED");
    }

    @Test
    void noThinkingOnNonGptModelMarksNo() {
        ObservabilityAdvisor advisor = advisor("claude-3", null, true, 32768);
        AssistantMessage msg = new AssistantMessage("答复");

        advisor.adviseCall(request(), chainReturning(new ChatClientResponse(
                chatResponse(msg, null, "stop"), new HashMap<>())));

        assertThat(lastSpan().attributes().get("thinking.available")).isEqualTo("NO");
    }

    @Test
    void explicitOpenAiProviderOverridesHeuristic() {
        // modelProvider 显式 openai：modelName 不含 gpt 也判 PROVIDER_NOT_RETURNED
        ObservabilityAdvisor advisor = advisor("claude-3", "openai", true, 32768);
        AssistantMessage msg = new AssistantMessage("答复");

        advisor.adviseCall(request(), chainReturning(new ChatClientResponse(
                chatResponse(msg, null, "stop"), new HashMap<>())));

        assertThat(lastSpan().attributes().get("thinking.available")).isEqualTo("PROVIDER_NOT_RETURNED");
    }

    @Test
    void hasToolCallsSuppressesFinalReply() {
        ObservabilityAdvisor advisor = advisor("gpt-4o", null, true, 32768);
        AssistantMessage msg = AssistantMessage.builder().content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("id1", "function", "read_file", "{}")))
                .build();

        advisor.adviseCall(request(), chainReturning(new ChatClientResponse(
                chatResponse(msg, null, "tool_calls"), new HashMap<>())));

        assertThat(eventTypes().stream().filter(s -> s.contains("FINAL_REPLY")).count()).isZero();
    }

    @Test
    void blankTextSuppressesFinalReply() {
        ObservabilityAdvisor advisor = advisor("gpt-4o", null, true, 32768);
        AssistantMessage msg = new AssistantMessage("  ");

        advisor.adviseCall(request(), chainReturning(new ChatClientResponse(
                chatResponse(msg, null, "stop"), new HashMap<>())));

        assertThat(eventTypes().stream().filter(s -> s.contains("FINAL_REPLY")).count()).isZero();
    }

    @Test
    void callChainExceptionMarksErrorAndRethrows() {
        ObservabilityAdvisor advisor = advisor("gpt-4o", null, true, 32768);
        RuntimeException boom = new RuntimeException("model boom");

        assertThatThrownBy(() -> advisor.adviseCall(request(), new CallAdvisorChain() {
            @Override
            public ChatClientResponse nextCall(ChatClientRequest req) {
                throw boom;
            }

            @Override
            public List<org.springframework.ai.chat.client.advisor.api.CallAdvisor> getCallAdvisors() {
                return List.of();
            }

            @Override
            public CallAdvisorChain copy(org.springframework.ai.chat.client.advisor.api.CallAdvisor a) {
                return this;
            }
        })).isSameAs(boom);

        assertThat(lastSpan().status()).isEqualTo(SpanStatus.ERROR);
    }

    @Test
    void truncatedThinkingPayloadCarriesTruncatedMarks() {
        // thinkingMaxChars=5：思维链截断 → payload 带 truncated + original.length
        ObservabilityAdvisor advisor = advisor("gpt-4o", null, true, 5);
        AssistantMessage msg = AssistantMessage.builder().content("答复")
                .properties(Map.of("thinking", "非常长的思维链内容超过五字"))
                .build();

        advisor.adviseCall(request(), chainReturning(new ChatClientResponse(
                chatResponse(msg, null, "stop"), new HashMap<>())));

        assertThat(lastSpan().attributes().get("thinking.available")).isEqualTo("YES");
        assertThat(eventPayloads().stream().anyMatch(s -> s.contains("truncated"))).isTrue();
    }

    @Test
    void snapshotCaptureDisabledSkipsSnapshotEnqueue() {
        ObservabilityAdvisor advisor = advisor("gpt-4o", null, false, 32768);
        int before = recorder.items.size();

        advisor.adviseCall(request(), chainReturning(new ChatClientResponse(
                chatResponse(new AssistantMessage("答"), null, "stop"), new HashMap<>())));

        // mc RUNNING + FINAL_REPLY 事件 + mc 终态：FINAL_REPLY 本就随 outcome 记录
        assertThat(recorder.items.size() - before).isEqualTo(3);
        assertThat(eventTypes()).contains("FINAL_REPLY");
    }

    @Test
    void snapshotCaptureWithoutSessionSpanSkipsEnqueue() {
        // state 未 onOpen：sessionSpan null → 快照跳过（不抛不队）
        recorder = new RecordingBase();
        ObservabilitySessionState bare = new ObservabilitySessionState(recorder, null, "sess-9", "a", "b", "gpt");
        ObservabilityAdvisor advisor = new ObservabilityAdvisor(recorder,
                new ObservabilityConfig(true, 200, Duration.ofSeconds(1), Duration.ofSeconds(5), 10000,
                        true, null, 32768, false, true, false, "openai"),
                new ThinkingChainExtractor(null, 32768),
                estimator(),
                bare, "gpt");
        int before = recorder.items.size();

        advisor.adviseCall(request(), chainReturning(new ChatClientResponse(
                chatResponse(new AssistantMessage("答"), null, "stop"), new HashMap<>())));

        // 无快照入队（sessionSpan null 分支）；MODEL_CALL RUNNING + FINAL_REPLY 事件 + 终态 = 3
        assertThat(recorder.items.size() - before).isEqualTo(3);
        assertThat(eventTypes()).contains("FINAL_REPLY");
    }
}
