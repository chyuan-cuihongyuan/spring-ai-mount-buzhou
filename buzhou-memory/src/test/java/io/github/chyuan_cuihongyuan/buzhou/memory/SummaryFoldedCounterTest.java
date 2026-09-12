package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 99 §B / T370：折入速率 counter 红队——folded 计数（tag trigger）随折入走；
 * breaker 开路跳过计数（fold-skipped）且无误报 folded。spec 95 fog 收口。
 */
class SummaryFoldedCounterTest {

    static class StubSummaryModel implements ChatModel {
        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            String text = """
                    ## USER_INTENT（用户核心诉求）
                    计数测试

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

    static final class CapturingMetrics implements BuzhouMetrics {
        final ConcurrentLinkedQueue<String> counters = new ConcurrentLinkedQueue<>();

        @Override
        public void counter(String name, long delta, String... tagKeyValue) {
            counters.add(name + ":" + String.join("=", tagKeyValue));
        }

        @Override
        public void timer(String name, Duration duration, String... tagKeyValue) {
        }
    }

    private CapturingMetrics metrics;
    private SummaryStoreBridge bridge;

    @BeforeEach
    void setUp() {
        metrics = new CapturingMetrics();
        BuzhouMetricsHolder.install(metrics);
        bridge = new SummaryStoreBridge(
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore());
    }

    @AfterEach
    void cleanup() {
        BuzhouMetricsHolder.reset();
    }

    private InjectionViewProcessor processor(int backlog, SummaryCircuitBreaker breaker) {
        InjectionViewProcessor ivp = new InjectionViewProcessor(
                new DefaultMicroCompactor(new DefaultCompletedTurnDetector()),
                t -> MicroCompactionPolicy.defaults(), 1,
                new DefaultBudgetCalculator(
                        new io.github.chyuan_cuihongyuan.buzhou.core.token.TableContextWindowResolver(Map.of()),
                        new io.github.chyuan_cuihongyuan.buzhou.core.token.CharHeuristicTokenEstimator()),
                bridge, new DefaultSummaryGenerator(), breaker,
                new StubSummaryModel(), "stub", 1, null, 1_000_000);
        ivp.setBoundaryCompactBacklog(backlog);
        return ivp;
    }

    private java.util.List<String> summaryCounters() {
        return metrics.counters.stream()
                .filter(c -> c.startsWith("buzhou.memory.summary.")).toList();
    }

    private static BuzhouMessage turnPair(int turn) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s1", turn, 0,
                Role.USER, "问题" + turn, List.of(), null, null, null, Map.of(), Instant.now());
    }

    @Test
    void foldedCounterCountsOnSuccessWithTag() {
        processor(1, new SummaryCircuitBreaker(3)).process("s1",
                List.of(turnPair(1)), 2);

        assertThat(summaryCounters()).containsExactly(
                "buzhou.memory.summary.folded:trigger=backlog");
    }

    @Test
    void breakerOpenCountsSkippedNotFolded() {
        // breaker 阈值 0 场景不可构造（阈值>=1）——用先失败烧断的 breaker：直接记录失败
        SummaryCircuitBreaker breaker = new SummaryCircuitBreaker(1);
        breaker.onFailure("s1"); // 烧断（阈值 1）
        assertThat(breaker.allows("s1")).isFalse();

        processor(1, breaker).process("s1", List.of(turnPair(1)), 2);

        assertThat(summaryCounters()).containsExactly(
                "buzhou.memory.summary.fold-skipped:trigger=backlog");
    }
}
