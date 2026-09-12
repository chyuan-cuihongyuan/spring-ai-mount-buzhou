package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 边界机会压缩测试（spec 70 §B / T292）：积压达标提前生成摘要（预算未压也出摘要块 +
 * coversUpTo 推进）；未达阈值不提前；默认（0=关）零变化。
 */
class BoundaryCompactBacklogTest {

    /** 固定九段摘要输出的 stub（merge 可解析）。 */
    static class StubSummaryModel implements ChatModel {
        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            String text = """
                    ## USER_INTENT（用户核心诉求）
                    边界压缩测试

                    ## CURRENT_STATE（当前工作现场）
                    积压已摘

                    ## NEXT_STEP（下一步）
                    无
                    """;
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    private InjectionViewProcessor processor(ChatModel summaryModel, int backlog) {
        InjectionViewProcessor ivp = new InjectionViewProcessor(
                new DefaultMicroCompactor(new DefaultCompletedTurnDetector()),
                t -> MicroCompactionPolicy.defaults(), 1,
                new DefaultBudgetCalculator(
                        new io.github.chyuan_cuihongyuan.buzhou.core.token.TableContextWindowResolver(Map.of()),
                        new io.github.chyuan_cuihongyuan.buzhou.core.token.CharHeuristicTokenEstimator()),
                new SummaryStoreBridge(new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore()),
                new DefaultSummaryGenerator(), new SummaryCircuitBreaker(3),
                summaryModel, "stub", 1, null, 1_000_000);
        ivp.setBoundaryCompactBacklog(backlog);
        return ivp;
    }

    private static BuzhouMessage turnPair(int turn) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s1", turn, 0,
                Role.USER, "问题" + turn, List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** 积压 4 ≥ 阈值 4：预算未压也提前出摘要块（结构化摘要注入）。 */
    @Test
    void backlogThresholdTriggersEarlySummary() {
        InjectionViewProcessor ivp = processor(new StubSummaryModel(), 4);
        List<BuzhouMessage> view = ivp.process("s1",
                List.of(turnPair(1), turnPair(2), turnPair(3), turnPair(4), turnPair(5)), 6);
        assertThat(view).anyMatch(m -> m.metadata().containsKey("summary")); // 提前摘要块出现
    }

    /** 积压 3 < 阈值 4：不提前（无摘要块——预算未压走 injectSummaryOnly 空注入）。 */
    @Test
    void belowThresholdDoesNotTrigger() {
        InjectionViewProcessor ivp = processor(new StubSummaryModel(), 4);
        List<BuzhouMessage> view = ivp.process("s1",
                List.of(turnPair(1), turnPair(2), turnPair(3)), 4);
        assertThat(view).noneMatch(m -> m.metadata().containsKey("summary"));
    }

    /** 默认（0=关）：同输入零变化（与既有行为一致——无提前摘要）。 */
    @Test
    void defaultDisabledKeepsBehavior() {
        InjectionViewProcessor ivp = processor(new StubSummaryModel(), 0);
        List<BuzhouMessage> view = ivp.process("s1",
                List.of(turnPair(1), turnPair(2), turnPair(3), turnPair(4), turnPair(5)), 6);
        assertThat(view).noneMatch(m -> m.metadata().containsKey("summary"));
    }
}
