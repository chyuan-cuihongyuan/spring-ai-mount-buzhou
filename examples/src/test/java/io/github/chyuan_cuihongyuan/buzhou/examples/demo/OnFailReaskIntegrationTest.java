package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T19「on_fail 动词汇 · REASK」端到端（docs/spec/11 guard）：可恢复失败（工具入参 schema 不合法）
 * 按 REASK 把结构化错误回喂模型自我纠错重试，且<b>有上界</b>（T17 有界 Turn 预算兜底，不无限循环）。
 */
class OnFailReaskIntegrationTest {

    @Test
    void invalidArgsAreFedBackAndModelSelfCorrects() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SelfCorrectingArgsModel model = new SelfCorrectingArgsModel();
        AtomicInteger calls = new AtomicInteger();
        // 可恢复 schema 失败的典型缝：工具侧入参校验（缺必填字段抛错 → T16 错误回喂 → 模型自纠重试）
        ToolCallback query = FunctionToolCallback.builder("query_order",
                        (java.util.function.Function<Map<String, Object>, String>) input -> {
                            if (!input.containsKey("orderId")) {
                                throw new IllegalArgumentException("orderId 必填");
                            }
                            return "ok-" + calls.incrementAndGet();
                        })
                .description("d")
                .inputType(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .inputSchema("{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"string\"}},\"required\":[\"orderId\"]}")
                .build();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(), query);

        AgentSession session = runtime.spawn("reask-app", "reask-agent", "reask-sess");
        String reply = session.chat("查订单 ORD-1");
        session.close();

        // 第一次坏参（缺 orderId）被 REASK 回喂（T16 结构化错误反馈），第二次自纠成功 → 工具恰好执行 1 次
        assertThat(model.errorFeedbackSeen).contains("[工具执行失败]").contains("orderId 必填");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(reply).contains("已查到");
    }

    @Test
    void neverCorrectingModelStopsAtReaskBudgetGracefully() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        NeverCorrectingModel model = new NeverCorrectingModel();
        ToolCallback query = FunctionToolCallback.builder("query_order",
                        (java.util.function.Function<Map<String, Object>, String>) input -> {
                            if (!input.containsKey("orderId")) {
                                throw new IllegalArgumentException("orderId 必填");
                            }
                            return "never";
                        })
                .description("d")
                .inputType(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .inputSchema("{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"string\"}},\"required\":[\"orderId\"]}")
                .build();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(), query);

        AgentSession session = runtime.spawn("reask-app", "reask-agent", "reask-budget-sess");
        String reply = session.chat("查订单 ORD-1");
        session.close();

        // REASK 有上界：模型永不修正时，T17 有界 Turn 预算兜底、优雅收尾（不无限烧 token）
        assertThat(reply).contains("预算内收尾");
        assertThat(model.calls.get()).isLessThanOrEqualTo(41);
    }

    /** 先发坏参（缺必填字段，REASK 一次），见到错误反馈后自纠。 */
    static final class SelfCorrectingArgsModel implements ChatModel {
        String errorFeedbackSeen;

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            List<Message> msgs = prompt.getInstructions();
            Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            if (last instanceof ToolResponseMessage toolResponse) {
                String data = toolResponse.getResponses().getFirst().responseData();
                if (data.contains("[工具执行失败]")) {
                    errorFeedbackSeen = data;
                    // 自纠：补上必填字段
                    return toolCall("query_order", "{\"orderId\":\"ORD-1\"}");
                }
                return text("已查到：" + data);
            }
            // 首次：坏参（用错字段名，触发工具侧必填校验失败）
            return toolCall("query_order", "{\"orderNo\":\"ORD-1\"}");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    /** 永不修正：持续发坏参（验证 REASK 上界由有界 Turn 兜底）。 */
    static final class NeverCorrectingModel implements ChatModel {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            calls.incrementAndGet();
            return toolCall("query_order", "{\"orderNo\":\"ORD-1\"}");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    private static ChatResponse text(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    private static ChatResponse toolCall(String name, String args) {
        return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                .content("").toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc", "function", name, args))).build())));
    }
}
