package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 122 / impl-271：superstep 原子批回归——四象限（开-违规中止整批 / 开-全过正常 /
 * 开-工具缺失中止 / 默认关行为不变）+ 事件日志 BATCH_ABORTED 结局断言。
 */
class AtomicSuperstepBatchTest {

    /** 非法入参样本：不是合法 JSON（validator 判「入参不是合法 JSON」）。 */
    private static final String BAD_ARGS = "oops-not-json";

    /** schema 具备可校验结构（object + properties）：合法入参为 {}。 */
    private ToolCallback strictTool(String name, AtomicInteger invocations) {
        return toolWithSchema(name, invocations, """
                {"type":"object","properties":{"x":{"type":"integer"}}}""");
    }

    private ToolCallback lenientTool(String name, AtomicInteger invocations) {
        return toolWithSchema(name, invocations, "{}");
    }

    private ToolCallback toolWithSchema(String name, AtomicInteger invocations, String schema) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema(schema).build();
            }

            @Override
            public String call(String toolInput) {
                invocations.incrementAndGet();
                return name + ":ok";
            }
        };
    }

    private HarnessToolCallingManager newManager() {
        return new HarnessToolCallingManager(
                DefaultToolCallingManager.builder().build(),
                Executors.newVirtualThreadPerTaskExecutor(), 8, Duration.ofSeconds(2), Map.of());
    }

    private ToolExecutionResult dispatch(HarnessToolCallingManager manager,
                                         List<ToolCallback> tools,
                                         AssistantMessage.ToolCall... calls) {
        AssistantMessage assistant = AssistantMessage.builder()
                .content("").toolCalls(List.of(calls)).build();
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(tools).build();
        return manager.executeToolCalls(new Prompt(List.of(), options), response);
    }

    private AssistantMessage.ToolCall toolCallOf(String id, String name, String arguments) {
        return new AssistantMessage.ToolCall(id, "function", name, arguments);
    }

    private ToolResponseMessage responsesOf(ToolExecutionResult result) {
        return (ToolResponseMessage) result.conversationHistory().getLast();
    }

    @Test
    void atomicAbortsWholeBatchWhenAnyArgsInvalid() {
        HarnessToolCallingManager manager = newManager();
        manager.setAtomicBatchValidation(true);
        AtomicInteger goodCalls = new AtomicInteger();
        List<ToolCallback> tools = List.of(
                strictTool("good", goodCalls), strictTool("bad", new AtomicInteger()));

        ToolExecutionResult result = dispatch(manager, tools,
                toolCallOf("1", "good", "{}"), toolCallOf("2", "bad", BAD_ARGS));

        ToolResponseMessage responses = responsesOf(result);
        assertThat(goodCalls.get()).isZero();
        assertThat(responses.getResponses().get(0).responseData())
                .contains("原子中止").contains("未执行");
        assertThat(responses.getResponses().get(1).responseData())
                .contains(ToolValidationFeedback.MARKER);
        assertThat(responses.getResponses())
                .extracting(ToolResponseMessage.ToolResponse::id)
                .containsExactly("1", "2");
    }

    @Test
    void atomicBatchExecutesNormallyWhenAllValid() {
        HarnessToolCallingManager manager = newManager();
        manager.setAtomicBatchValidation(true);
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();

        ToolExecutionResult result = dispatch(manager,
                List.of(strictTool("a", a), strictTool("b", b)),
                toolCallOf("1", "a", "{}"), toolCallOf("2", "b", "{}"));

        assertThat(a.get()).isEqualTo(1);
        assertThat(b.get()).isEqualTo(1);
        assertThat(responsesOf(result).getResponses())
                .extracting(ToolResponseMessage.ToolResponse::responseData)
                .containsExactly("a:ok", "b:ok");
    }

    @Test
    void atomicAbortsBatchWhenToolMissing() {
        HarnessToolCallingManager manager = newManager();
        manager.setAtomicBatchValidation(true);
        AtomicInteger goodCalls = new AtomicInteger();

        ToolExecutionResult result = dispatch(manager, List.of(lenientTool("good", goodCalls)),
                toolCallOf("1", "good", "{}"), toolCallOf("2", "ghost", "{}"));

        assertThat(goodCalls.get()).isZero();
        ToolResponseMessage responses = responsesOf(result);
        assertThat(responses.getResponses().get(0).responseData()).contains("原子中止");
        assertThat(responses.getResponses().get(1).responseData())
                .contains(ToolErrorFeedback.missingToolReason("ghost"));
    }

    @Test
    void defaultOffKeepsPerToolBehavior() {
        HarnessToolCallingManager manager = newManager();
        AtomicInteger goodCalls = new AtomicInteger();
        List<ToolCallback> tools = List.of(
                strictTool("good", goodCalls), strictTool("bad", new AtomicInteger()));

        ToolExecutionResult result = dispatch(manager, tools,
                toolCallOf("1", "good", "{}"), toolCallOf("2", "bad", BAD_ARGS));

        assertThat(goodCalls.get()).isEqualTo(1);
        ToolResponseMessage responses = responsesOf(result);
        assertThat(responses.getResponses().get(0).responseData()).isEqualTo("good:ok");
        assertThat(responses.getResponses().get(1).responseData())
                .contains(ToolValidationFeedback.MARKER);
    }

    @Test
    void batchAbortedOutcomeRecordedToEventLog() {
        HarnessToolCallingManager manager = new HarnessToolCallingManager(
                DefaultToolCallingManager.builder().build(),
                Executors.newVirtualThreadPerTaskExecutor(), 8, Duration.ofSeconds(2), Map.of(),
                null, "sess-atomic");
        manager.setAtomicBatchValidation(true);
        RecordingLog log = new RecordingLog();
        manager.setToolCallLog(log);
        AtomicInteger goodCalls = new AtomicInteger();

        dispatch(manager,
                List.of(strictTool("good", goodCalls), strictTool("bad", new AtomicInteger())),
                toolCallOf("1", "good", "{}"), toolCallOf("2", "bad", BAD_ARGS));

        Optional<ToolCallLogEntry> aborted = log.find("sess-atomic", "1");
        assertThat(aborted).isPresent();
        assertThat(aborted.get().outcome()).isEqualTo(ToolCallOutcome.BATCH_ABORTED);
        Optional<ToolCallLogEntry> rejected = log.find("sess-atomic", "2");
        assertThat(rejected).isPresent();
        assertThat(rejected.get().outcome()).isEqualTo(ToolCallOutcome.VALIDATION_REJECTED);
        assertThat(goodCalls.get()).isZero();
    }

    /** 进程内事件日志（仅测试：嵌套 map 存储，find 按 (sessionId, toolCallId) 查最后一条）。 */
    private static final class RecordingLog implements ToolCallLog {
        private final Map<String, Map<String, ToolCallLogEntry>> entries = new ConcurrentHashMap<>();

        @Override
        public void append(ToolCallLogEntry entry) {
            entries.computeIfAbsent(entry.sessionId(), k -> new ConcurrentHashMap<>())
                    .put(entry.toolCallId(), entry);
        }

        @Override
        public Optional<ToolCallLogEntry> find(String sessionId, String toolCallId) {
            return Optional.ofNullable(entries.getOrDefault(sessionId, Map.of()).get(toolCallId));
        }
    }
}
