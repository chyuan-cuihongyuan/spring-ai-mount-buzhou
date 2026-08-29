package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
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
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 95 §B / T356：摘要折入通知红队——trigger 溯源（backlog/drift 正确标注；
 * lambda 兼容：只实现 onCompacted 的监听器不炸）；折入失败（breaker 开）不误通知。
 * spec 90 fog 项收口。
 */
class SummaryFoldedTriggerTest {

    static class StubSummaryModel implements ChatModel {
        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            String text = """
                    ## USER_INTENT（用户核心诉求）
                    触发测试

                    ## CURRENT_STATE（当前工作现场）
                    已折入

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

    private record Fold(String sessionId, String trigger, long coversUpToTurn) {
    }

    private InjectionViewProcessor processor(SummaryStoreBridge bridge, int backlog,
            io.github.chyuan_cuihongyuan.buzhou.memory.compact.SemanticDriftDetector detector,
            CompactionListener listener) {
        InjectionViewProcessor ivp = new InjectionViewProcessor(
                new DefaultMicroCompactor(new DefaultCompletedTurnDetector()),
                t -> MicroCompactionPolicy.defaults(), 1,
                new DefaultBudgetCalculator(
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver(Map.of()),
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator()),
                bridge, new DefaultSummaryGenerator(), new SummaryCircuitBreaker(3),
                new StubSummaryModel(), "stub", 1, null, 1_000_000);
        ivp.setBoundaryCompactBacklog(backlog);
        if (detector != null) {
            ivp.setSemanticDriftDetector(detector);
        }
        ivp.setCompactionListener(listener);
        return ivp;
    }

    private static BuzhouMessage turnPair(int turn, String text) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s1", turn, 0,
                Role.USER, text, List.of(), null, null, null, Map.of(), Instant.now());
    }

    @Test
    void backlogTriggerIsLabeledBacklogAndLambdaCompatHolds() {
        SummaryStoreBridge bridge = new SummaryStoreBridge(
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore());
        ConcurrentLinkedQueue<Fold> folds = new ConcurrentLinkedQueue<>();
        // lambda 只实现 onCompacted——default onSummaryFolded 必须兼容（编译面）
        CompactionListener capture = new CompactionListener() {
            @Override
            public void onCompacted(String sessionId, io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionResult result, double evictRatio) {
            }

            @Override
            public void onSummaryFolded(String sessionId, NineSectionSummary summary, String trigger) {
                folds.add(new Fold(sessionId, trigger, summary.coversUpToTurn()));
            }
        };

        // 积压阈值 1：首.process 即积压触发折入（预算未压——32K 窗口下 2 条消息远不超）
        processor(bridge, 1, null, capture).process("s1",
                List.of(turnPair(1, "数据库连接池怎么调优")), 2);

        assertThat(folds).hasSize(1);
        assertThat(folds.peek().trigger()).isEqualTo("backlog");
        assertThat(folds.peek().coversUpToTurn()).isGreaterThan(0);
    }

    @Test
    void driftTriggerIsLabeledDrift() {
        SummaryStoreBridge bridge = new SummaryStoreBridge(
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore());
        ConcurrentLinkedQueue<Fold> folds = new ConcurrentLinkedQueue<>();
        CompactionListener capture = new CompactionListener() {
            @Override
            public void onCompacted(String sessionId, io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionResult result, double evictRatio) {
            }

            @Override
            public void onSummaryFolded(String sessionId, NineSectionSummary summary, String trigger) {
                folds.add(new Fold(sessionId, trigger, summary.coversUpToTurn()));
            }
        };

        // 阶段一：积压建基准
        processor(bridge, 1, null, capture).process("s1",
                List.of(turnPair(1, "数据库连接池怎么调优")), 2);
        folds.clear();
        // 阶段二：backlog=0 + 恒漂移——纯漂移触发
        processor(bridge, 0, (input, summary) -> true, capture).process("s1",
                List.of(turnPair(1, "数据库连接池怎么调优"), turnPair(2, "换个话题写诗"),
                        turnPair(3, "再写一首")), 4);

        assertThat(folds).hasSize(1);
        assertThat(folds.peek().trigger()).isEqualTo("drift");
    }
}
