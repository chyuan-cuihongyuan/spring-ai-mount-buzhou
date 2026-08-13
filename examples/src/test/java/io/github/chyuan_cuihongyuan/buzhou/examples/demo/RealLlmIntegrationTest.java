package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 LLM 集成测试 —— 真实 API 变体（impl/06 / T5）：凭据门控、CI 默认跳过、仅本地带 key 跑。
 *
 * <p>与 {@link RealBehaviorIntegrationTest}（反应式 mock、CI 绿）覆盖<b>同一条 core 链</b>，但用
 * <b>真实模型</b>（OpenAI 兼容）。{@code @SpringBootTest} 仅用于经 {@code spring-ai-starter-model-openai}
 * 自动装配得到 {@link ChatModel} bean（从 {@code spring.ai.openai.api-key} 等属性）；Buzhou 仍纯编程式装配
 * （不依赖 buzhou starter）。
 *
 * <p><b>凭据门控</b>：无 {@code BUZHOU_LLM_API_KEY} 时本测试整体禁用（不加载 Spring 上下文、不触网），CI 不红。
 * <b>本地运行</b>：{@code export BUZHOU_LLM_API_KEY=sk-...}（可选 {@code BUZHOU_LLM_BASE_URL} 指向兼容网关、
 * {@code BUZHOU_LLM_MODEL} 默认 {@code gpt-4o-mini}）后 {@code mvn -pl examples test -Dtest=RealLlmIntegrationTest}。
 *
 * <p>防脆性：真实模型输出不可预测，故断言为弱断言（回复非空、消息已持久、链路不抛异常），不做精确文本匹配；
 * 真实模型行为偏差由人工复核。
 */
@SpringBootTest(classes = RealLlmIntegrationTest.TestApp.class, properties = {
        "spring.ai.openai.api-key=${BUZHOU_LLM_API_KEY}",
        "spring.ai.openai.base-url=${BUZHOU_LLM_BASE_URL:https://api.openai.com}",
        "spring.ai.openai.chat.options.model=${BUZHOU_LLM_MODEL:gpt-4o-mini}"
})
@EnabledIfEnvironmentVariable(named = "BUZHOU_LLM_API_KEY", matches = ".+")
class RealLlmIntegrationTest {

    @Autowired
    ChatModel chatModel;

    @Test
    void realModelRunsToolAndMemoryChain() {
        // 纯编程式 Buzhou 装配（真实 ChatModel 来自 Spring 自动装配）
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "real-llm";
        // 预置若干历史，使真实模型对话在窗口约束下也会经过记忆管线
        stores.messageStore().append(sid, BuzhouDemo.seedHistory(sid, 4));
        RuntimeConfig config = MemoryModule.configure(Map.of(), stores.messageStore());
        ToolCallback getOrderStatus = fixedTool("get_order_status",
                "[日志] 订单 " + BuzhouDemo.ORDER_ID + " 错误码 " + BuzhouDemo.ERROR_CODE
                        + " " + "查询行数据".repeat(160));
        AgentRuntime runtime = Buzhou.runtime(chatModel, stores, config, getOrderStatus);

        AgentSession session = runtime.spawn("real-llm-app", "support-agent", sid);
        String reply = session.chat("帮我查一下订单 " + BuzhouDemo.ORDER_ID + " 为什么失败");
        session.close();

        // 弱断言（真实模型输出不可预测）：链路端到端跑通、不抛异常、有回复、消息已持久
        assertThat(reply).as("真实模型应返回非空回复").isNotBlank();
        assertThat(stores.messageStore().load(sid))
                .as("会话消息应已持久").isNotEmpty();
    }

    private static ToolCallback fixedTool(String name, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("查订单状态")
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    /** 最小 Spring Boot 上下文：仅承载 OpenAI starter 的 ChatModel 自动装配。 */
    @SpringBootApplication
    static class TestApp {
    }
}
