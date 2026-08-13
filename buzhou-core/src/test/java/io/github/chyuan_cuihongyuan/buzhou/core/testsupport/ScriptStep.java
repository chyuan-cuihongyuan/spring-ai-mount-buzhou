package io.github.chyuan_cuihongyuan.buzhou.core.testsupport;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link FakeChatModel} 脚本中的一步：一条 assistant 回复（可同时含文本与多个工具调用）。
 *
 * <p>{@link #parallel(ToolCallSpec...)} 是<b>并行工具调用块</b>的关键语义——单条 assistant 消息
 * 携带多个 toolCall，Spring AI 工具调用循环会将其 fan-out 并行执行（buzhou-core 虚拟线程），
 * 这正是回放「并行工具」行为所需的脚本单元。
 */
public final class ScriptStep {

    private final String text;
    private final List<ToolCallSpec> toolCalls;

    ScriptStep(String text, List<ToolCallSpec> toolCalls) {
        this.text = text;
        this.toolCalls = List.copyOf(toolCalls);
    }

    /** 纯文本回复（终局消息）。 */
    public static ScriptStep text(String content) {
        return new ScriptStep(content, List.of());
    }

    /** 单工具调用回复（content 为空、仅 toolCall）。 */
    public static ScriptStep toolCall(String name, String arguments) {
        return new ScriptStep("", List.of(new ToolCallSpec(name, arguments)));
    }

    /** 并行工具调用块：单条 assistant 消息携带多个 toolCall（并行 fan-out 语义）。 */
    public static ScriptStep parallel(ToolCallSpec... calls) {
        return new ScriptStep("", List.of(calls));
    }

    /** 文本 + 工具调用并存（部分 provider 会同时输出 content 与 tool_calls）。 */
    public static ScriptStep of(String text, ToolCallSpec... calls) {
        return new ScriptStep(text, List.of(calls));
    }

    /** 生成 ChatResponse；toolCall id 按 {@code tc-<callIndex>-<k>} 确定性派生（回放自洽）。 */
    ChatResponse toChatResponse(int callIndex) {
        AssistantMessage message;
        if (toolCalls.isEmpty()) {
            message = new AssistantMessage(text == null ? "" : text);
        } else {
            List<AssistantMessage.ToolCall> calls = new ArrayList<>();
            int k = 0;
            for (ToolCallSpec spec : toolCalls) {
                calls.add(new AssistantMessage.ToolCall(
                        "tc-" + callIndex + "-" + k++, "function", spec.name(), spec.arguments()));
            }
            message = AssistantMessage.builder()
                    .content(text == null ? "" : text)
                    .toolCalls(calls)
                    .build();
        }
        return new ChatResponse(List.of(new Generation(message)));
    }

    String text() {
        return text;
    }

    List<ToolCallSpec> toolCalls() {
        return toolCalls;
    }
}
