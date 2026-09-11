package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
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
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工具幂等键传播测试（spec 608 / T866–T867 / impl 461，Stripe X-Idempotency-Key
 * 思想）：同批两调用各见含自身 callId 的键；同 callId 重复派发键恒同（重试去重锚）。
 */
class IdempotencyKeyPropagationTest {

    /** 记录所见幂等键的探针工具。 */
    private ToolCallback probeTool(String name, ConcurrentLinkedQueue<String> seen) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput, org.springframework.ai.chat.model.ToolContext ctx) {
                seen.add(HarnessToolCallingManager.idempotencyKeyOf(ctx));
                return name + ":ok";
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("本测试走 context 路径");
            }
        };
    }

    private ToolExecutionResult dispatch(HarnessToolCallingManager manager, ToolCallback tool,
            String... callIds) {
        List<AssistantMessage.ToolCall> calls = java.util.Arrays.stream(callIds)
                .map(id -> new AssistantMessage.ToolCall(id, "function", "probe", "{}")).toList();
        AssistantMessage assistant = AssistantMessage.builder()
                .content("").toolCalls(calls).build();
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));
        return manager.executeToolCalls(new Prompt(List.of(),
                ToolCallingChatOptions.builder().toolCallbacks(List.of(tool)).build()), response);
    }

    private static HarnessToolCallingManager manager(String sessionId) {
        return new HarnessToolCallingManager(
                DefaultToolCallingManager.builder().build(),
                java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor(),
                8, Duration.ofSeconds(5), Map.of(), null, sessionId);
    }

    /** 同批两调用：各见含自身 callId 的键（互不相同）。 */
    @Test
    void eachCallSeesItsOwnKey() {
        HarnessToolCallingManager manager = manager("sess-1");
        ConcurrentLinkedQueue<String> seen = new ConcurrentLinkedQueue<>();

        dispatch(manager, probeTool("probe", seen), "call-a", "call-b");

        assertThat(seen).hasSize(2);
        assertThat(seen).containsExactlyInAnyOrder("sess-1:call-a", "sess-1:call-b");
    }

    /** 同一逻辑调用重复派发（重试模拟）：键恒同——上游去重锚稳定。 */
    @Test
    void sameCallIdYieldsStableKeyAcrossDispatches() {
        HarnessToolCallingManager manager = manager("sess-1");
        ConcurrentLinkedQueue<String> seen = new ConcurrentLinkedQueue<>();

        dispatch(manager, probeTool("probe", seen), "call-x");
        dispatch(manager, probeTool("probe", seen), "call-x");

        assertThat(seen).containsExactly("sess-1:call-x", "sess-1:call-x");
    }

    /** 无会话绑定的独立使用：anon 缺省（诚实降级，不空键）。 */
    @Test
    void anonymousSessionFallsBackToAnonPrefix() {
        HarnessToolCallingManager manager = manager(null);
        ConcurrentLinkedQueue<String> seen = new ConcurrentLinkedQueue<>();

        dispatch(manager, probeTool("probe", seen), "call-y");

        assertThat(seen).containsExactly("anon:call-y");
    }
}
