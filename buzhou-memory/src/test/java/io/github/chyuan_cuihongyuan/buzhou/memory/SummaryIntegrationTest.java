package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryIntegrationTest {

    private List<BuzhouMessage> thirtyTurnSession(String sessionId) {
        List<BuzhouMessage> history = new ArrayList<>();
        for (int turn = 1; turn <= 30; turn++) {
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 0,
                    Role.USER, "排查订单 ORD-1 第 " + turn + " 步：" + "x".repeat(500),
                    List.of(), null, null, null, Map.of(), Instant.now()));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 1,
                    Role.ASSISTANT, "已执行第 " + turn + " 步：" + "y".repeat(500),
                    List.of(), null, null, null, Map.of(), Instant.now()));
        }
        return history;
    }

    private ChatModel mainModel(List<String> prompts) {
        return new ChatModel() {
            @Override
            public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
                return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
            }

            @Override
            public ChatResponse call(Prompt prompt) {
                prompts.add(prompt.getInstructions().toString());
                return new ChatResponse(List.of(new Generation(new AssistantMessage("收到"))));
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.just(call(prompt));
            }
        };
    }

    private ChatModel summaryModel(String sections) {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage(sections))));
            }
        };
    }

    private static final String NINE_SECTIONS = """
            ## USER_INTENT
            排查订单 ORD-1 支付卡单
            ## CURRENT_STATE
            已推进到第 28 步
            ## NEXT_STEP
            检查网关回调
            ## PENDING_TASKS
            无
            ## ERRORS_FIXES
            无
            ## KEY_ARTIFACTS
            流水号 PAY-9
            ## PROBLEM_SOLVING
            定位到网关
            ## TECHNICAL_CONCEPTS
            支付状态机
            ## USER_MESSAGES_LOG
            若干提问
            """;

    private Map<String, Object> smallWindowYml() {
        return Map.of("model-name", "test-model",
                "memory", Map.of("context-window", Map.of("test-model", 13000),
                        "keep-recent-turns", 2));
    }

    @Test
    void overBudgetTriggersSummaryAndP0SurvivesAfterThirtyTurns() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "sum-" + UUID.randomUUID();
        stores.messageStore().append(sessionId, thirtyTurnSession(sessionId));

        List<String> prompts = new CopyOnWriteArrayList<>();
        RuntimeConfig config = MemoryModule.configure(smallWindowYml(), stores,
                mainModel(prompts), summaryModel(NINE_SECTIONS));
        AgentRuntime runtime = Buzhou.runtime(mainModel(prompts), stores, config);
        AgentSession session = runtime.spawn("app", "agent", sessionId);
        session.chat("继续排查");

        String injected = prompts.getFirst();
        assertThat(injected).contains("<system-reminder>");
        assertThat(injected).contains("USER_INTENT").contains("排查订单 ORD-1 支付卡单");
        assertThat(injected).contains("NEXT_STEP");
        assertThat(stores.summaryStore().latest(sessionId)).isPresent();

        session.chat("第 31 轮");
        assertThat(prompts.get(1)).contains("USER_INTENT");
        session.close();
    }

    @Test
    void summaryModelFailureFallsBackToRawHistory() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "sum-fail-" + UUID.randomUUID();
        stores.messageStore().append(sessionId, thirtyTurnSession(sessionId));

        List<String> prompts = new CopyOnWriteArrayList<>();
        AtomicInteger summaryCalls = new AtomicInteger();
        ChatModel brokenSummary = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                summaryCalls.incrementAndGet();
                throw new IllegalStateException("summary model down");
            }
        };
        RuntimeConfig config = MemoryModule.configure(smallWindowYml(), stores,
                mainModel(prompts), brokenSummary);
        AgentRuntime runtime = Buzhou.runtime(mainModel(prompts), stores, config);
        AgentSession session = runtime.spawn("app", "agent", sessionId);

        String reply = session.chat("继续");
        assertThat(reply).isEqualTo("收到");
        assertThat(prompts.getFirst()).contains("排查订单 ORD-1 第 1 步");

        session.chat("再来");
        session.chat("第三次");
        session.chat("熔断后");
        assertThat(summaryCalls.get()).isEqualTo(3);
        session.close();
    }

    @Test
    void secondCompactionMergesWithPreviousGeneration() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "sum-merge-" + UUID.randomUUID();
        stores.messageStore().append(sessionId, thirtyTurnSession(sessionId));

        List<String> summaryPrompts = new CopyOnWriteArrayList<>();
        ChatModel mergingSummary = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                summaryPrompts.add(prompt.getInstructions().getFirst().getText());
                return new ChatResponse(List.of(new Generation(new AssistantMessage(NINE_SECTIONS))));
            }
        };
        List<String> prompts = new CopyOnWriteArrayList<>();
        Map<String, Object> yml = Map.of("model-name", "test-model",
                "memory", Map.of("context-window", Map.of("test-model", 9000),
                        "keep-recent-turns", 28));
        RuntimeConfig config = MemoryModule.configure(yml, stores, mainModel(prompts), mergingSummary);
        AgentRuntime runtime = Buzhou.runtime(mainModel(prompts), stores, config);
        AgentSession session = runtime.spawn("app", "agent", sessionId);
        session.chat("t31");
        session.chat("t32");

        assertThat(summaryPrompts).hasSizeGreaterThanOrEqualTo(2);
        assertThat(summaryPrompts.get(1)).contains("合并更新").contains("排查订单 ORD-1 支付卡单");
        session.close();
    }
}
