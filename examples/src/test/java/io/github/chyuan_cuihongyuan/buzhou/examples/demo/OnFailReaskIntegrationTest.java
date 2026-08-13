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
 * T19「on_fail 动词汇 · REASK」端到端（docs/spec/11 guard + wayfinder2 impl-04/T30 强化）：
 * 可恢复失败按 REASK 回喂自纠重试且<b>有上界</b>——坏参现在被<b>执行前 schema 校验</b>更早拦截
 * （`[工具参数校验失败]`，工具零误执行），与执行期错误（`[工具执行失败]`）两档词汇分明；
 * 永不修正时由 per-Turn 重试预算（T30）先于 Turn 上界优雅收尾。
 */
class OnFailReaskIntegrationTest {

    @Test
    void invalidArgsAreFedBackAndModelSelfCorrects() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SelfCorrectingArgsModel model = new SelfCorrectingArgsModel();
        AtomicInteger calls = new AtomicInteger();
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

        // 坏参被<b>执行前</b> schema 校验拦截（REASK 通道：参数校验失败词汇 + 缺失字段说明），
        // 模型自纠后成功——工具恰好执行 1 次（坏参零误执行）
        assertThat(model.errorFeedbackSeen).contains("[工具参数校验失败]").contains("orderId");
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

        // REASK 有上界：永不修正时由 T30 重试预算（默认 2）先于 Turn 上界 REASK_FAILED 优雅收尾
        assertThat(reply).contains("重试预算上限");
        assertThat(model.calls.get()).isLessThanOrEqualTo(4);
    }

    /** 先发坏参（触发执行前 schema 校验反馈），见到校验反馈后自纠。 */
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
                if (data.contains("[工具参数校验失败]") || data.contains("[工具执行失败]")) {
                    errorFeedbackSeen = data;
                    // 自纠：补上必填字段
                    return toolCall("query_order", "{\"orderId\":\"ORD-1\"}");
                }
                return text("已查到：" + data);
            }
            // 首次：坏参（用错字段名，触发执行前 schema 校验失败）
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
