package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.LexicalDriftDetector;
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
 * spec 90 §B / T344：语义漂移触发压缩红队——漂移提前折入摘要（coversUpTo 水位推进
 * 真信号）、同话题不触发、默认零变化、词面检测器判定面。借鉴：Letta 语义触发压缩。
 */
class SemanticDriftCompactTest {

    static class TopicSummaryModel implements ChatModel {
        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            String text = """
                    ## USER_INTENT（用户核心诉求）
                    数据库连接池调优

                    ## CURRENT_STATE（当前工作现场）
                    已定位慢查询与池配置

                    ## NEXT_STEP（下一步）
                    调整池参数
                    """;
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    private InjectionViewProcessor processor(ChatModel summaryModel,
            io.github.chyuan_cuihongyuan.buzhou.memory.compact.SemanticDriftDetector detector,
            int backlog, SummaryStoreBridge bridge) {
        InjectionViewProcessor ivp = new InjectionViewProcessor(
                new DefaultMicroCompactor(new DefaultCompletedTurnDetector()),
                t -> MicroCompactionPolicy.defaults(), 1,
                new DefaultBudgetCalculator(
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver(Map.of()),
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator()),
                bridge, new DefaultSummaryGenerator(), new SummaryCircuitBreaker(3),
                summaryModel, "stub", 1, null, 1_000_000);
        ivp.setBoundaryCompactBacklog(backlog);
        if (detector != null) {
            ivp.setSemanticDriftDetector(detector);
        }
        return ivp;
    }

    private static BuzhouMessage turnPair(int turn, String text) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s1", turn, 0,
                Role.USER, text, List.of(), null, null, null, Map.of(), Instant.now());
    }

    private SummaryStoreBridge seedSummary() {
        SummaryStoreBridge bridge = new SummaryStoreBridge(
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore());
        processor(new TopicSummaryModel(), null, 1, bridge).process("s1",
                List.of(turnPair(1, "数据库连接池怎么调优")), 2);
        assertThat(bridge.loadLatest("s1")).isPresent();
        return bridge;
    }

    @Test
    void driftedInputTriggersEarlySummary() {
        SummaryStoreBridge bridge = seedSummary();
        int covered = bridge.loadLatest("s1").orElseThrow().coversUpToTurn();

        // backlog=0（积压判据不可用）+ 恒漂移 stub——纯漂移触发路径
        processor(new TopicSummaryModel(), (input, summary) -> true, 0, bridge)
                .process("s1", List.of(turnPair(1, "数据库连接池怎么调优"),
                        turnPair(2, "慢查询在哪"), turnPair(3, "换个话题写首诗")), 4);

        // 真信号：摘要水位推进（漂移触发新折入）
        assertThat(bridge.loadLatest("s1").orElseThrow().coversUpToTurn()).isGreaterThan(covered);
    }

    @Test
    void sameTopicDoesNotTrigger() {
        SummaryStoreBridge bridge = seedSummary();
        int covered = bridge.loadLatest("s1").orElseThrow().coversUpToTurn();

        processor(new TopicSummaryModel(), (input, summary) -> false, 0, bridge)
                .process("s1", List.of(turnPair(1, "数据库连接池怎么调优"),
                        turnPair(2, "池参数再看看")), 3);

        // 不漂移：水位不动（无新折入）
        assertThat(bridge.loadLatest("s1").orElseThrow().coversUpToTurn()).isEqualTo(covered);
    }

    @Test
    void noDetectorMeansZeroChange() {
        SummaryStoreBridge bridge = seedSummary();
        int covered = bridge.loadLatest("s1").orElseThrow().coversUpToTurn();

        processor(new TopicSummaryModel(), null, 0, bridge)
                .process("s1", List.of(turnPair(1, "数据库连接池怎么调优"),
                        turnPair(2, "换个话题写首诗")), 3);

        // 无检测器：漂移路径零变化
        assertThat(bridge.loadLatest("s1").orElseThrow().coversUpToTurn()).isEqualTo(covered);
    }

    @Test
    void lexicalDetectorJudgesByBigramOverlap() {
        LexicalDriftDetector detector = new LexicalDriftDetector();
        String summary = "数据库连接池调优 慢查询 池配置";

        assertThat(detector.drifted("数据库连接池参数怎么调", summary)).isFalse(); // 同话题
        assertThat(detector.drifted("帮我写一首关于春天的诗", summary)).isTrue(); // 全异话题

        // 阈值方向：0.0 = 永不判漂移（similarity < 0 恒假）；1.0 = 极端敏感
        assertThat(new LexicalDriftDetector(0.0).drifted("帮我写诗", summary)).isFalse();
        assertThat(new LexicalDriftDetector(1.0).drifted("数据库连接池调优", summary)).isTrue();

        // 空基准不判漂移（诚实：缺证据不动作）
        assertThat(detector.drifted("任何输入", null)).isFalse();
        assertThat(detector.drifted(" ", summary)).isFalse();
    }
}
