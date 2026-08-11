package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery.DedupGate;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery.DedupRecorder;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.IdempotencyKeyExtractor;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.IdempotencyKeys;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 幂等去重地基（ticket 03）：执行脊柱 reserve/fill + 同键第二次不重执行。
 * 用工具调用计数器从外部证明「恰好一次」，去重命中经事件留痕。
 */
class HarnessToolCallingManagerDedupTest {

    private static final String SESSION = "sess-dedup";

    private final InMemorySessionStateStore stateStore = new InMemorySessionStateStore();
    private final List<SessionEvent> events = new CopyOnWriteArrayList<>();

    private HarnessToolCallingManager manager(Map<String, IdempotencyKeyExtractor> extractors) {
        DedupGate gate = new DedupGate(new DedupRecorder(stateStore), extractors, events::add);
        return new HarnessToolCallingManager(DefaultToolCallingManager.builder().build(),
                Executors.newVirtualThreadPerTaskExecutor(), 8, Duration.ofSeconds(5),
                Map.of(), null, SESSION, gate);
    }

    private static ToolCallback tool(String name, AtomicInteger calls, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name).inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                calls.incrementAndGet();
                return result;
            }
        };
    }

    private static AssistantMessage.ToolCall call(String id, String name) {
        return new AssistantMessage.ToolCall(id, "function", name, "{}");
    }

    private static ToolExecutionResult execute(HarnessToolCallingManager manager,
                                               ToolCallback tool,
                                               AssistantMessage.ToolCall... calls) {
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(tool).build();
        AssistantMessage assistant = AssistantMessage.builder()
                .content("").toolCalls(List.of(calls)).build();
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));
        return manager.executeToolCalls(new Prompt(List.of(), options), response);
    }

    private static String resultOf(ToolExecutionResult result) {
        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        return responses.getResponses().getFirst().responseData();
    }

    @Test
    void sameKeySecondCallReturnsFirstResultWithoutReExecution() {
        AtomicInteger calls = new AtomicInteger();
        ToolCallback charge = tool("charge", calls, "charged-100");
        HarnessToolCallingManager manager = manager(Map.of());

        // 第一次：正常执行并回填
        String first = resultOf(execute(manager, charge, call("tc-1", "charge")));
        assertThat(first).isEqualTo("charged-100");
        assertThat(calls).hasValue(1);

        // 同会话同键（同 toolCallId）第二次：命中去重、返回首次结果、不重执行
        String second = resultOf(execute(manager, charge, call("tc-1", "charge")));
        assertThat(second).isEqualTo("charged-100");
        assertThat(calls).hasValue(1);
        assertThat(events).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("dedup-hit");
            assertThat(e.payload()).containsEntry("toolName", "charge")
                    .containsEntry("key", IdempotencyKeys.defaultKey("charge", "tc-1"));
        });
    }

    @Test
    void businessKeyExtractorOverridesDefaultKey() {
        AtomicInteger calls = new AtomicInteger();
        // 业务工具：键提取器从入参取订单号
        class OrderTool implements ToolCallback, IdempotencyKeyExtractor {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("charge").description("c").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                calls.incrementAndGet();
                return "charged";
            }

            @Override
            public String extractKey(String toolInput, ToolContext toolContext) {
                return "ORD-1";
            }
        }
        OrderTool orderTool = new OrderTool();
        HarnessToolCallingManager manager = manager(Map.of("charge", orderTool));

        // 两次不同 toolCallId、同订单号 → 同业务键 → 第二次不重执行
        execute(manager, orderTool, call("tc-1", "charge"));
        String second = resultOf(execute(manager, orderTool, call("tc-2", "charge")));
        assertThat(second).isEqualTo("charged");
        assertThat(calls).hasValue(1);
        assertThat(events).anySatisfy(e -> assertThat(e.payload())
                .containsEntry("key", IdempotencyKeys.businessKey("charge", "ORD-1")));
    }

    @Test
    void failedExecutionReleasesPendingSoRetryReExecutes() {
        AtomicInteger calls = new AtomicInteger();
        ToolCallback flaky = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("flaky").description("f").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                if (calls.incrementAndGet() == 1) {
                    throw new IllegalStateException("boom");
                }
                return "ok";
            }
        };
        HarnessToolCallingManager manager = manager(Map.of());

        // 第一次失败：释放 pending 记录（不回填失败文本），允许重试重新 reserve
        String first = resultOf(execute(manager, flaky, call("tc-1", "flaky")));
        assertThat(first).startsWith("执行失败");
        // 重试同键：记录已释放 → 重新执行成功
        String second = resultOf(execute(manager, flaky, call("tc-1", "flaky")));
        assertThat(second).isEqualTo("ok");
        assertThat(calls).hasValue(2);
    }
}
