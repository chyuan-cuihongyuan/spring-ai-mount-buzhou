package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-02 / T36 evictRatio 集成：默认 0.7 部分逐出；预算仍超时按 10% 步进梯子加压；
 * 梯子救回预算 → 免落摘要折叠（保连续优先于折摘要）。
 */
class EvictRatioLadderTest {

    private static final String NINE_SECTIONS = """
            ## USER_INTENT
            排查订单 ORD-7
            ## CURRENT_STATE
            已到第 5 步
            ## NEXT_STEP
            检查网关
            ## PENDING_TASKS
            无
            ## ERRORS_FIXES
            无
            ## KEY_ARTIFACTS
            PAY-7
            ## PROBLEM_SOLVING
            定位超时
            ## TECHNICAL_CONCEPTS
            状态机
            ## USER_MESSAGES_LOG
            若干
            """;

    /** 10 个完结轮：候选轮（turn 1..7）大结果 9000 字符；受保护近期轮（8..10）小结果。 */
    private List<BuzhouMessage> bigToolHistory(String sessionId) {
        List<BuzhouMessage> history = new ArrayList<>();
        String big = "x".repeat(9000);
        for (int turn = 1; turn <= 10; turn++) {
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 0,
                    Role.USER, "第 " + turn + " 步", List.of(), null, null, null, Map.of(), Instant.now()));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 1,
                    Role.ASSISTANT, "", List.of(new ToolCallRecord("tc-" + turn, "query", "{}")),
                    null, null, null, Map.of(), Instant.now()));
            // maxAgeTurns(3) 内的近期轮结果保持小体量——它们不是候选、永远内联
            String result = turn <= 7 ? big : "ok";
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 2,
                    Role.TOOL, result, List.of(), "tc-" + turn, null, null,
                    Map.of("toolName", "query"), Instant.now()));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 3,
                    Role.ASSISTANT, "完成 " + turn, List.of(), null, null, null, Map.of(), Instant.now()));
        }
        return history;
    }

    @Test
    void ladderEscalatesEvictionAndRescuesBudgetWithoutSummaryFolding() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "ladder-" + UUID.randomUUID();
        stores.messageStore().append(sessionId, bigToolHistory(sessionId));

        List<String> prompts = new CopyOnWriteArrayList<>();
        ChatModel main = recordingModel(prompts);
        // 窗口 13000 − 输出预留 8000 − 安全缓冲 3000 = 历史预算 2000 token：
        // 0.7 档保留 2 条 9000 字符内联（≈4500 token）仍超 → 0.8（保 1 条，≈2250 token）仍超
        // → 0.9（ceil(6.3)=7 全逐出）达标——梯子救回预算、免落摘要折叠
        Map<String, Object> yml = Map.of("model-name", "tiny",
                "memory", Map.of("context-window", Map.of("tiny", 13000),
                        "keep-recent-turns", 2));
        RuntimeConfig config = MemoryModule.configure(yml, stores, main,
                summaryModel(NINE_SECTIONS));

        AgentRuntime runtime = Buzhou.runtime(main, stores, config);
        AgentSession session = runtime.spawn("ladder-app", "agent", sessionId);
        session.chat("继续");
        session.close();

        String injected = prompts.getFirst();
        // 梯子已加压到全量逐出：7 个候选全部为 evidence 占位符
        assertThat(countOccurrences(injected, "evidence-id=")).isEqualTo(7);
        // 梯子救回预算 → 未落摘要折叠（无 system-reminder 摘要块、摘要存储为空）
        assertThat(injected).doesNotContain("<system-reminder>");
        assertThat(stores.summaryStore().latest(sessionId)).isEmpty();
    }

    private static ChatModel recordingModel(List<String> prompts) {
        return new ChatModel() {
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

    private static ChatModel summaryModel(String sections) {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage(sections))));
            }
        };
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
