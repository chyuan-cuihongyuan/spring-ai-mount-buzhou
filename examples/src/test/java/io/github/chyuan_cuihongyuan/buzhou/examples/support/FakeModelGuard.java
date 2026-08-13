package io.github.chyuan_cuihongyuan.buzhou.examples.support;

import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.TestDoubleChatModel;
import org.springframework.ai.chat.model.ChatModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端测试「防真实请求」门（impl-01）：入场断言模型必须是测试替身
 * （FakeChatModel / ScriptedChatModel / RecordingChatModel 等 {@link TestDoubleChatModel}），
 * 误配真实模型时在发第一个请求前即失败——Pydantic AI {@code ALLOW_MODEL_REQUESTS=False}
 * 语义的 Buzhou 落地。真实 API 行为由 {@code RealLlmIntegrationTest} 凭据门控单独覆盖。
 */
public final class FakeModelGuard {

    private FakeModelGuard() {
    }

    /** 断言给定模型是测试替身；否则给出可读失败信息。 */
    public static void requireTestDouble(ChatModel model) {
        assertThat(model)
                .as("端到端测试防真实请求门：模型必须是 TestDoubleChatModel（FakeChatModel/ScriptedChatModel/RecordingChatModel）；"
                        + "真实模型请走 RealLlmIntegrationTest 的凭据门控路径")
                .isInstanceOf(TestDoubleChatModel.class);
    }
}
