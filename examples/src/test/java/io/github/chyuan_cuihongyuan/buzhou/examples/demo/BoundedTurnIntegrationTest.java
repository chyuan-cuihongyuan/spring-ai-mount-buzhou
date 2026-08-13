package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.TurnLoopPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T17「有界 Turn」端到端（docs/spec/11 core）：模型陷入工具调用死循环时，
 * Turn 在预算内终止并产出优雅最终回复——成本与延迟有硬上限，非崩溃、非无限烧 token。
 */
class BoundedTurnIntegrationTest {

    @Test
    void runawayToolLoopStopsWithinBudgetWithGracefulFinal() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        RunawayMockModel model = new RunawayMockModel();
        CountingTool tool = new CountingTool();
        RuntimeConfig config = RuntimeConfig.turnLoopPolicy(TurnLoopPolicy.of(3));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config, tool);

        AgentSession session = runtime.spawn("bound-app", "runaway-agent", "runaway-sess");
        String reply = session.chat("帮我反复核查直到我说停");
        session.close();

        // 优雅收尾：回复是兜底文案，Turn 正常完成
        assertThat(reply).contains("预算内收尾");
        // 硬上界：3 轮工具执行后第 4 轮被拦截（模型共被调 4 次），不会无限烧下去
        assertThat(tool.invocations.get()).isEqualTo(3);
        assertThat(model.calls.get()).isEqualTo(4);
    }

    /** 失控模拟：模型永远只发工具调用（runaway 死循环语义）。 */
    static final class RunawayMockModel implements ChatModel {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            int n = calls.incrementAndGet();
            return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                    .content("").toolCalls(List.of(new AssistantMessage.ToolCall(
                            "tc-" + n, "function", "probe_tool", "{}"))).build())));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    static final class CountingTool implements ToolCallback {
        final AtomicInteger invocations = new AtomicInteger();

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name("probe_tool").description("d")
                    .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
        }

        @Override
        public String call(String toolInput) {
            return "probe-" + invocations.incrementAndGet();
        }
    }
}
