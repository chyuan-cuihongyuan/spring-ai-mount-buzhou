package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1526 / T2303–T2304：批级工具结果回喂预算——批内总量超限时按响应长度降序
 * 贪心截大者（保留小结果完整），截断件带标记与保留量；预算 0 = 关零行为。
 * manager 级直测（HarnessToolCallingManagerTest 脚手架同款）。
 */
class BatchResponseBudgetTest {

    @AfterEach
    void tearDown() {
        BatchResponseBudgetHolder.reset();
    }

    private static ToolCallback fixedLenTool(String name, int len) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("t")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return "x".repeat(len);
            }
        };
    }

    private static HarnessToolCallingManager manager() {
        return new HarnessToolCallingManager(
                org.springframework.ai.model.tool.DefaultToolCallingManager.builder().build(),
                Executors.newVirtualThreadPerTaskExecutor(), 8, Duration.ofSeconds(5),
                Map.of(), new io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier(),
                "s-batch-budget");
    }

    private org.springframework.ai.model.tool.ToolExecutionResult execute(
            HarnessToolCallingManager manager, List<ToolCallback> tools,
            AssistantMessage.ToolCall... calls) {
        AssistantMessage assistant = AssistantMessage.builder()
                .content("").toolCalls(List.of(calls)).build();
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(tools).build();
        return manager.executeToolCalls(new Prompt(List.of(), options), response);
    }

    /** 预算 60：批内大(100)+小(20) → 大者截至 20 内（总量 40 ≤ 60），小者完整。 */
    @Test
    void batchBudgetShouldTruncateLargestFirstKeepingSmallIntact() {
        HarnessToolCallingManager manager = manager();
        manager.setBatchResponseBudget(60);

        org.springframework.ai.model.tool.ToolExecutionResult result = execute(manager,
                List.of(fixedLenTool("big_tool", 100), fixedLenTool("small_tool", 20)),
                new AssistantMessage.ToolCall("1", "function", "big_tool", "{}"),
                new AssistantMessage.ToolCall("2", "function", "small_tool", "{}"));

        List<org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse> responses =
                ((org.springframework.ai.chat.messages.ToolResponseMessage) result.conversationHistory().getLast()).getResponses();
        String big = responses.stream().filter(r -> "big_tool".equals(r.name()))
                .findFirst().orElseThrow().responseData();
        String small = responses.stream().filter(r -> "small_tool".equals(r.name()))
                .findFirst().orElseThrow().responseData();
        // 大者被截：含标记 + 保留量说明
        assertThat(big).contains("[批级预算截断").contains("保留");
        // 小者完整（20 字符无标记）
        assertThat(small).isEqualTo("x".repeat(20));
    }

    /** 预算 0（默认）：零截断零行为。 */
    @Test
    void zeroBudgetShouldBeNoop() {
        HarnessToolCallingManager manager = manager(); // 未设预算

        org.springframework.ai.model.tool.ToolExecutionResult result = execute(manager,
                List.of(fixedLenTool("big_tool", 10_000)),
                new AssistantMessage.ToolCall("1", "function", "big_tool", "{}"));

        String big = ((org.springframework.ai.chat.messages.ToolResponseMessage) result.conversationHistory().getLast()).getResponses().get(0).responseData();
        assertThat(big).isEqualTo("x".repeat(10_000)); // 完整透传
    }

    /** spec 1540 / T2331：FAILED_ONLY 组合——同伴失败占位文本（短）在批预算下保留。 */
    @Test
    void failedOnlyPlaceholderShouldSurviveBatchBudget() {
        HarnessToolCallingManager manager = manager();
        manager.setBatchResponseBudget(80);
        manager.setBatchFeedbackPolicy(
                HarnessToolCallingManager.BatchFeedbackPolicy.FAILED_ONLY);
        ToolCallback failing = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("boom").description("t")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("boom");
            }
        };

        org.springframework.ai.model.tool.ToolExecutionResult result = execute(manager,
                List.of(failing, fixedLenTool("big_tool", 400)),
                new AssistantMessage.ToolCall("1", "function", "boom", "{}"),
                new AssistantMessage.ToolCall("2", "function", "big_tool", "{}"));

        List<org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse> responses =
                ((org.springframework.ai.chat.messages.ToolResponseMessage)
                        result.conversationHistory().getLast()).getResponses();
        String placeholder = responses.stream().filter(r -> "big_tool".equals(r.name()))
                .findFirst().orElseThrow().responseData();
        // FAILED_ONLY 占位文本（成功者以短提示替代）不在批预算截断中丢失
        assertThat(placeholder).contains("本批有同伴失败").doesNotContain("[批级预算截断");
    }

    /** spec 1527 / T2305：错误反馈豁免——超限批内错误反馈（结构化纠错信号）完整保留。 */
    @Test
    void errorFeedbackShouldBeExemptFromTruncation() {
        HarnessToolCallingManager manager = manager();
        manager.setBatchResponseBudget(50);
        // 错误反馈长 120（超预算本身）+ 大结果 200：错误反馈豁免，大结果被截
        ToolCallback failing = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("failing").description("t")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("boom"); // 错误即反馈通道合成
            }
        };

        org.springframework.ai.model.tool.ToolExecutionResult result = execute(manager,
                List.of(failing, fixedLenTool("big_tool", 200)),
                new AssistantMessage.ToolCall("1", "function", "failing", "{}"),
                new AssistantMessage.ToolCall("2", "function", "big_tool", "{}"));

        List<org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse> responses =
                ((org.springframework.ai.chat.messages.ToolResponseMessage)
                        result.conversationHistory().getLast()).getResponses();
        String feedback = responses.stream().filter(r -> "failing".equals(r.name()))
                .findFirst().orElseThrow().responseData();
        String big = responses.stream().filter(r -> "big_tool".equals(r.name()))
                .findFirst().orElseThrow().responseData();
        assertThat(feedback).doesNotContain("[批级预算截断"); // 错误反馈完整
        assertThat(big).contains("[批级预算截断"); // 大结果承担截断
    }

    /** 总量未超限：零截断（阈值边界不误伤）。 */
    @Test
    void withinBudgetShouldNotTruncate() {
        HarnessToolCallingManager manager = manager();
        manager.setBatchResponseBudget(200);

        org.springframework.ai.model.tool.ToolExecutionResult result = execute(manager,
                List.of(fixedLenTool("a", 100), fixedLenTool("b", 90)),
                new AssistantMessage.ToolCall("1", "function", "a", "{}"),
                new AssistantMessage.ToolCall("2", "function", "b", "{}"));

        List<org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse> responses =
                ((org.springframework.ai.chat.messages.ToolResponseMessage) result.conversationHistory().getLast()).getResponses();
        assertThat(responses).noneMatch(r -> r.responseData().contains("[批级预算截断"));
    }
}
