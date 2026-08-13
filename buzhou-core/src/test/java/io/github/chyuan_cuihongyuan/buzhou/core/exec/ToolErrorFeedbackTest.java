package io.github.chyuan_cuihongyuan.buzhou.core.exec;

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
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.ParameterizedTypeReference;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T16「错误即反馈」行为测试：工具异常/缺失时合成结构化 ToolResponse（错误文案 + 原入参），
 * Turn 不死、每个 tool_call 恒有一个响应、按原序回注模型。
 */
class ToolErrorFeedbackTest {

    private static final String ARGS_ORDER = "{\"orderId\":\"ORD-7\"}";

    @Test
    void formatContainsToolNameArgumentsReasonAndGuidance() {
        String feedback = ToolErrorFeedback.format("flaky", ARGS_ORDER, "执行失败：disk-full");
        assertThat(feedback).contains("[工具执行失败]");
        assertThat(feedback).contains("工具：flaky");
        assertThat(feedback).contains("入参：");
        assertThat(feedback).contains(ARGS_ORDER);
        assertThat(feedback).contains("执行失败：disk-full");
        assertThat(feedback).contains("建议：");
    }

    @Test
    void blankArgumentsRenderAsEmptyJson() {
        String feedback = ToolErrorFeedback.format("t", "  ", "执行超时（60s）");
        assertThat(feedback).contains("入参：{}");
        assertThat(feedback).contains("执行超时（60s）");
    }

    @Test
    void exceptionPathFeedsBackArgumentsToModel() {
        HarnessToolCallingManager manager = manager();
        ToolCallback broken = failingTool("flaky", new IllegalStateException("disk-full"));

        ToolExecutionResult result = runToolCalls(manager, List.of(broken), toolCallOf("1", "flaky", ARGS_ORDER));

        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(responses.getResponses()).hasSize(1);
        String feedback = responses.getResponses().getFirst().responseData();
        assertThat(feedback).contains("工具：flaky");
        assertThat(feedback).contains(ARGS_ORDER);
        assertThat(feedback).contains("disk-full");
    }

    @Test
    void missingToolPathFeedsBackInsteadOfCrashing() {
        ToolExecutionResult result = runToolCalls(manager(), List.of(), toolCallOf("1", "no_such_tool", ARGS_ORDER));

        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        String feedback = responses.getResponses().getFirst().responseData();
        assertThat(feedback).contains("未知工具：no_such_tool");
        assertThat(feedback).contains(ARGS_ORDER);
    }

    @Test
    void catastrophicErrorStillYieldsOneResponsePerToolCallInOrder() {
        ToolCallback fatal = errorTool("fatal", "catastrophic");
        ToolCallback fine = constantTool("fine", "fine-result");

        ToolExecutionResult result = runToolCalls(manager(), List.of(fatal, fine),
                toolCallOf("1", "fatal", ARGS_ORDER), toolCallOf("2", "fine", "{}"));

        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(responses.getResponses()).hasSize(2);
        assertThat(responses.getResponses().get(0).id()).isEqualTo("1");
        assertThat(responses.getResponses().get(0).responseData()).contains("catastrophic");
        assertThat(responses.getResponses().get(1).responseData()).contains("fine-result");
    }

    private HarnessToolCallingManager manager() {
        return new HarnessToolCallingManager(
                DefaultToolCallingManager.builder().build(),
                Executors.newVirtualThreadPerTaskExecutor(), 8, Duration.ofSeconds(1), Map.of());
    }

    private ToolExecutionResult runToolCalls(HarnessToolCallingManager manager,
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

    /** 经参数化构造器注册一个必然抛错的工具（模拟执行异常/致命错误两类失败注入）。 */
    private ToolCallback failingTool(String name, RuntimeException failure) {
        return FunctionToolCallback.builder(name,
                        (java.util.function.Function<Map<String, Object>, String>) input -> {
                            throw failure;
                        })
                .description("d")
                .inputType(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .inputSchema("{}")
                .build();
    }

    /** 抛 Error 的工具：executeOne 的 catch(Exception) 接不住，验证聚合层兜底仍产出每调用一个响应。 */
    private ToolCallback errorTool(String name, String message) {
        return FunctionToolCallback.builder(name,
                        (java.util.function.Function<Map<String, Object>, String>) input -> {
                            throw new AssertionError(message);
                        })
                .description("d")
                .inputType(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .inputSchema("{}")
                .build();
    }

    private ToolCallback constantTool(String name, String result) {
        return FunctionToolCallback.builder(name,
                        (java.util.function.Function<Map<String, Object>, String>) input -> result)
                .description("d")
                .inputType(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .inputSchema("{}")
                .build();
    }

    /** 既有文案子串兼容：超时/取消/中断路径仍含既有关键词。 */
    @Test
    void legacyReasonKeywordsPreserved() {
        assertThat(ToolErrorFeedback.format("t", "{}", "执行超时（60s）")).contains("执行超时");
        assertThat(ToolErrorFeedback.format("t", "{}", "执行已取消")).contains("执行已取消");
        assertThat(ToolErrorFeedback.format("t", "{}", "执行被中断")).contains("执行被中断");
        assertThat(ToolErrorFeedback.missingToolReason("nope")).contains("未知工具：nope");
    }
}
