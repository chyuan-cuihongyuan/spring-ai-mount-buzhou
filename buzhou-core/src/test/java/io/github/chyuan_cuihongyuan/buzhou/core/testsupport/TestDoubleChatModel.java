package io.github.chyuan_cuihongyuan.buzhou.core.testsupport;

import org.springframework.ai.chat.model.ChatModel;

/**
 * 测试替身模型标记接口（随 core test-jar 发布）。
 *
 * <p>端到端测试可在入场处断言 {@code model instanceof TestDoubleChatModel}（见 examples 的
 * {@code FakeModelGuard}），实现「防真实请求」门：未注册替身而误用真实模型时，
 * 测试在发第一个请求前即失败，而非打到真实 API 才发现（Pydantic AI
 * {@code ALLOW_MODEL_REQUESTS=False} 同款语义的 Buzhou 落地）。
 */
public interface TestDoubleChatModel extends ChatModel {
}
