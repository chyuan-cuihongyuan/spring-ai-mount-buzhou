package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 308 / impl-331：deadline 跨工具传播回归——工具读到正剩余（动态视图）/
 * 无 Deadline 哨兵放行 / 助手对缺键异型返回哨兵。
 */
class DeadlinePropagationTest {

    private static final long DEADLINE_MILLIS = 5000L;

    /** 记录所见 deadline 的自限工具（每页 1ms——模拟按剩余收敛）。 */
    private ToolCallback selfLimitingTool(String name, ConcurrentLinkedQueue<TurnDeadline> seen) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput, org.springframework.ai.chat.model.ToolContext ctx) {
                seen.add(HarnessToolCallingManager.turnDeadlineOf(ctx));
                return name + ":ok";
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("本测试走 context 路径");
            }
        };
    }

    private ToolExecutionResult dispatch(HarnessToolCallingManager manager, ToolCallback tool,
            String... names) {
        List<AssistantMessage.ToolCall> calls = java.util.Arrays.stream(names)
                .map(n -> new AssistantMessage.ToolCall(n, "function", n, "{}")).toList();
        AssistantMessage assistant = AssistantMessage.builder()
                .content("").toolCalls(calls).build();
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));
        return manager.executeToolCalls(new Prompt(List.of(),
                ToolCallingChatOptions.builder().toolCallbacks(List.of(tool)).build()), response);
    }

    @Test
    void toolSeesLiveDeadline_whenSet() {
        HarnessToolCallingManager manager = new HarnessToolCallingManager(
                DefaultToolCallingManager.builder().build(),
                java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor(),
                8, Duration.ofSeconds(5), Map.of());
        manager.beginTurn(TurnDeadline.at(Instant.now().plusMillis(DEADLINE_MILLIS)));
        ConcurrentLinkedQueue<TurnDeadline> seen = new ConcurrentLinkedQueue<>();

        dispatch(manager, selfLimitingTool("probe", seen), "probe");

        assertThat(seen).hasSize(1);
        TurnDeadline deadline = seen.peek();
        assertThat(deadline.isNone()).isFalse();
        assertThat(deadline.remainingMillis()).isPositive();
        assertThat((Object) deadline).isSameAs(manager.turnDeadline());
    }

    @Test
    void toolSeesNoneSentinel_whenNoDeadline() {
        HarnessToolCallingManager manager = new HarnessToolCallingManager(
                DefaultToolCallingManager.builder().build(),
                java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor(),
                8, Duration.ofSeconds(5), Map.of());
        ConcurrentLinkedQueue<TurnDeadline> seen = new ConcurrentLinkedQueue<>();

        dispatch(manager, selfLimitingTool("probe", seen), "probe");

        assertThat(seen).hasSize(1);
        assertThat(seen.peek().isNone())
                .as("未设 Deadline：哨兵放行（工具自由收敛步数）").isTrue();
    }

    @Test
    void helperReturnsSentinelForMissingOrAlienValues() {
        assertThat(HarnessToolCallingManager.turnDeadlineOf(null).isNone()).isTrue();
        assertThat(HarnessToolCallingManager.turnDeadlineOf(
                new org.springframework.ai.chat.model.ToolContext(Map.of())).isNone()).isTrue();
        assertThat(HarnessToolCallingManager.turnDeadlineOf(
                new org.springframework.ai.chat.model.ToolContext(
                        Map.of(HarnessToolCallingManager.TURN_DEADLINE_KEY, "alien")))
                .isNone()).isTrue();
    }

    @Test
    void responsesDeliveredNormally() {
        HarnessToolCallingManager manager = new HarnessToolCallingManager(
                DefaultToolCallingManager.builder().build(),
                java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor(),
                8, Duration.ofSeconds(5), Map.of());
        ConcurrentLinkedQueue<TurnDeadline> seen = new ConcurrentLinkedQueue<>();

        ToolExecutionResult result = dispatch(manager, selfLimitingTool("probe", seen), "probe");
        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(responses.getResponses().get(0).responseData()).isEqualTo("probe:ok");
    }
}
