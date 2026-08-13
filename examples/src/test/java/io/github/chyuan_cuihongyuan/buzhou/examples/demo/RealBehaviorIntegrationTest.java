package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import io.github.chyuan_cuihongyuan.buzhou.memory.tool.EvidenceLookupTool;
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
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实行为集成测试 —— Mock 变体（impl/06 / T5）：进默认 {@code mvn verify}、CI 绿、无 key。
 *
 * <p>不同于 {@link BuzhouDemoTest} 的脚本化 stub（预入队固定回复），这里用 <b>反应式 mock</b>——
 * 模型按输入<b>决定</b>是否调工具：见用户排查请求且尚无工具结果 → 发 {@code get_order_status} 工具调用；
 * 收到工具结果 → 出文本小结。这更贴近真实 LLM 的工具调用决策行为，端到端跑通 core 的工具调用循环
 * （模型决策 → Harness 执行工具 → 结果回注 → 模型再决策）+ 多轮 + 微压缩 + evidence 回查。
 *
 * <p>防脆性：确定性反应式 mock，无网络、无 key。真实 API 行为由 {@link RealLlmIntegrationTest}
 * 凭据门控、CI 跳过、仅本地带 key 跑（同一 core 链）。
 */
class RealBehaviorIntegrationTest {

    @Test
    void reactiveMockDrivesToolLoopCompactionAndEvidenceLookup() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "real-behavior";
        // 预置 8 轮历史（每轮大工具返回）——足以在默认窗口触发微压缩，压缩旧轮
        stores.messageStore().append(sid, BuzhouDemo.seedHistory(sid, 8));

        ReactiveMockModel model = new ReactiveMockModel();
        RuntimeConfig config = MemoryModule.configure(Map.of(), stores.messageStore());
        // 真实工具：返回大日志（含预埋事实），供反应式 mock 调用、并被微压缩
        ToolCallback getOrderStatus = fixedTool("get_order_status",
                "[实时] 订单 " + BuzhouDemo.ORDER_ID + " 错误码 " + BuzhouDemo.ERROR_CODE
                        + " " + "查询行数据".repeat(160));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config, getOrderStatus);

        AgentSession session = runtime.spawn("real-app", "support-agent", sid);
        session.chat("继续排查"); // 反应式 mock：决策调工具 → Harness 执行 → 结果回注 → 小结
        session.close();

        // 1. 真实行为——模型按输入「决策」了工具调用，且见到了工具结果（工具调用循环协同）
        assertThat(model.emittedToolCalls).containsExactly("get_order_status");
        assertThat(model.sawToolResult)
                .as("工具调用循环：模型应在工具结果回注后被再次调用")
                .isTrue();

        // 2. 微压缩：旧轮大工具返回在注入视图中被压缩为 evidence 占位符
        String view = model.seenPrompts.get(0).getInstructions().toString();
        assertThat(view).contains("旧工具结果已清理").contains("evidence-id=");

        // 3. evidence 回查：占位符的 evidence-id 取回原文（含订单号 + 错误码）
        String evidenceId = extractEvidenceId(view);
        assertThat(evidenceId).isNotNull();
        String original = new EvidenceLookupTool(stores.messageStore())
                .call("{\"evidenceId\":\"" + evidenceId + "\"}");
        assertThat(original).contains(BuzhouDemo.ORDER_ID).contains(BuzhouDemo.ERROR_CODE);
    }

    private static String extractEvidenceId(String view) {
        Matcher m = Pattern.compile("evidence-id=([\\w-]+)").matcher(view);
        return m.find() ? m.group(1) : null;
    }

    private static ToolCallback fixedTool(String name, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    /** 反应式 mock：按输入决定工具调用 vs 文本小结（贴近真实 LLM 工具调用决策）。 */
    static final class ReactiveMockModel implements ChatModel {
        final List<Prompt> seenPrompts = new ArrayList<>();
        final List<String> emittedToolCalls = new ArrayList<>();
        boolean sawToolResult = false;

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            seenPrompts.add(prompt);
            List<Message> msgs = prompt.getInstructions();
            Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            if (last instanceof ToolResponseMessage) {
                // 工具结果已回注 → 出文本小结（真实模型收 tool 结果后总结的行为）
                sawToolResult = true;
                return text("已根据日志定位：网关层超时（" + BuzhouDemo.ERROR_CODE + "）");
            }
            // 无工具结果 → 决策调 get_order_status（真实模型按用户意图决策工具的行为）
            emittedToolCalls.add("get_order_status");
            return toolCall("get_order_status", "{}");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
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
}
