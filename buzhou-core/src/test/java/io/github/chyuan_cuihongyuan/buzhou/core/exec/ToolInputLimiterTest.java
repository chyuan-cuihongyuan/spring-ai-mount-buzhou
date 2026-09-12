package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 506 / T763：工具入参限幅——默认关闭零行为变化、超限拒绝（不回显
 * 入参）、per-tool glob 覆盖、会话 E2E（超限不执行工具、反馈回喂 REASK）。
 */
class ToolInputLimiterTest {

    @AfterEach
    void resetHolder() {
        ToolInputLimiterHolder.set(ToolInputLimiter.disabled());
    }

    @Test
    void disabledByDefaultAndZeroBehaviorChange() {
        assertThat(ToolInputLimiterHolder.current()
                .violation("any_tool", "x".repeat(1_000_000))).isNull();
    }

    @Test
    void oversizedInputRejectedWithoutEcho() {
        ToolInputLimiter limiter = new ToolInputLimiter(10, Map.of());
        String bigArgs = "{\"q\":\"" + "x".repeat(50) + "\"}";
        String violation = limiter.violation("search", bigArgs);
        assertThat(violation)
                .contains("入参超限").contains("search").contains("未执行")
                .doesNotContain(bigArgs); // 不回显入参——回显即重新入上下文
        assertThat(limiter.violation("search", "{\"q\":1}")).isNull();
    }

    @Test
    void perToolGlobOverridesWin() {
        ToolInputLimiter limiter = new ToolInputLimiter(10, Map.of(
                "web_*", -1, "tiny", 2));
        assertThat(limiter.limitFor("web_fetch")).isEqualTo(-1);
        assertThat(limiter.violation("web_fetch", "x".repeat(100))).isNull();
        assertThat(limiter.limitFor("tiny")).isEqualTo(2);
        assertThat(limiter.violation("tiny", "abc")).contains("未执行");
    }

    @Test
    void negativeDefaultRejected() {
        assertThatThrownBy(() -> new ToolInputLimiter(-2, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void oversizedToolCallFeedsStructuredFeedbackWithoutExecuting() {
        ToolInputLimiterHolder.set(new ToolInputLimiter(10, Map.of()));
        try {
            var model = new io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel();
            String bigArgs = "{\"q\":\"" + "x".repeat(80) + "\"}";
            model.enqueue(AssistantMessage.builder()
                    .content("")
                    .toolCalls(java.util.List.of(new AssistantMessage.ToolCall(
                            "tc-1", "function", "big_tool", bigArgs)))
                    .build());
            model.enqueueText("已按反馈精简");
            AtomicBoolean executed = new AtomicBoolean();
            ToolCallback tool = new ToolCallback() {
                @Override
                public ToolDefinition getToolDefinition() {
                    return ToolDefinition.builder()
                            .name("big_tool").description("大参数工具")
                            .inputSchema("{\"type\":\"object\"}").build();
                }

                @Override
                public String call(String toolInput) {
                    executed.set(true);
                    return "不应到达";
                }
            };
            var runtime = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.runtime(
                    model, io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores(), tool);
            var session = runtime.spawn("app", "agent", "sess-in");
            String reply = session.chat("调工具");
            session.close();
            assertThat(reply).isEqualTo("已按反馈精简");
            assertThat(executed.get()).isFalse(); // 超限工具未真正执行
            // 第二次模型调用收到结构化反馈（REASK 通道）
            assertThat(model.seenPrompts.get(1).toString()).contains("入参超限");
        } finally {
            ToolInputLimiterHolder.set(ToolInputLimiter.disabled());
        }
    }
}
