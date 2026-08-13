package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.FakeModelGuard;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T16「工具错误回喂模型」端到端（docs/spec/11 core）：单个工具抛异常时 Turn 不死——
 * 模型收到结构化错误反馈（错误文案 + 原工具入参 + 纠错建议）后自我纠错、继续完成请求；
 * 工具缺失同样回喂而非崩溃。模型侧异常仍照常上抛（与工具侧通道正交、互不吞没）。
 *
 * <p>impl-01 迁移：原本地反应式 mock 换成 {@link FakeChatModel} 脚本驱动
 * （决策序固定：先调工具 → 收到反馈后总结），断言不弱化。
 */
class ToolErrorFeedbackIntegrationTest {

    private static final String FLAKY_ARGS = "{\"orderId\":\"ORD-9\"}";
    private static final String FINAL_REPLY = "已根据错误反馈改用兜底方案完成排查";

    @Test
    void toolExceptionFeedsBackAndTurnCompletes() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("flaky_query", FLAKY_ARGS),
                ScriptStep.text(FINAL_REPLY));
        FakeModelGuard.requireTestDouble(model);
        ToolCallback flaky = throwingTool("flaky_query", "disk-full");
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(), flaky);

        AgentSession session = runtime.spawn("err-app", "support-agent", "err-session");
        String reply = session.chat("查一下这个订单的物流状态");
        session.close();

        // Turn 完成：模型收到错误反馈后给出最终回复（而非整轮失败）
        assertThat(reply).isEqualTo(FINAL_REPLY);
        // 模型确实见到了结构化错误反馈：错误文案 + 原入参 + 纠错建议
        String feedback = errorFeedbackSeen(model);
        assertThat(feedback).contains("[工具执行失败]");
        assertThat(feedback).contains("flaky_query");
        assertThat(feedback).contains(FLAKY_ARGS);
        assertThat(feedback).contains("disk-full");
        assertThat(feedback).contains("建议：");
    }

    @Test
    void missingToolFeedsBackInsteadOfCrashing() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("hallucinated_tool", FLAKY_ARGS),
                ScriptStep.text(FINAL_REPLY));
        FakeModelGuard.requireTestDouble(model);
        // 不注册 hallucinated_tool——模型调用了不存在的工具名
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());

        AgentSession session = runtime.spawn("err-app", "support-agent", "missing-tool-session");
        String reply = session.chat("帮我查订单");
        session.close();

        assertThat(reply).isEqualTo(FINAL_REPLY);
        String feedback = errorFeedbackSeen(model);
        assertThat(feedback).contains("未知工具：hallucinated_tool");
        assertThat(feedback).contains(FLAKY_ARGS);
    }

    @Test
    void modelSideExceptionStillPropagates() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        ChatModel brokenModel = new ChatModel() {
            @Override
            public ChatOptions getOptions() {
                return ToolCallingChatOptions.builder().build();
            }

            @Override
            public ChatResponse call(Prompt prompt) {
                throw new IllegalStateException("model-provider-down");
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.error(new IllegalStateException("model-provider-down"));
            }
        };
        AgentRuntime runtime = Buzhou.runtime(brokenModel, stores, RuntimeConfig.defaults());

        AgentSession session = runtime.spawn("err-app", "support-agent", "model-error-session");
        // 模型侧异常不属于工具侧「错误即反馈」通道——必须照常暴露（边界正交、互不吞没）
        assertThatThrownBy(() -> session.chat("随便问点啥"))
                .hasMessageContaining("model-provider-down");
        session.close();
    }

    /** 第二次模型调用回注的末条 ToolResponseMessage = 错误反馈载体。 */
    private static String errorFeedbackSeen(FakeChatModel model) {
        Message last = model.seenPrompts.get(1).getInstructions().getLast();
        assertThat(last).isInstanceOf(ToolResponseMessage.class);
        return ((ToolResponseMessage) last).getResponses().getFirst().responseData();
    }

    private static ToolCallback throwingTool(String name, String errorMessage) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException(errorMessage);
            }
        };
    }
}
