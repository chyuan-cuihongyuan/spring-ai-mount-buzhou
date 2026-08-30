package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 延迟感知备模型排序红队（spec 64 §B / T280）：EMA 更新与传导、未知延迟中位数中性、
 * 稳定并列、排序视图不改入参、E2E 降级时备模型延迟入账。
 */
class FallbackLatencyTrackerTest {

    private static NamedFallbackModel named(String name) {
        return new NamedFallbackModel(name, new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("r"))));
            }
        });
    }

    @Test
    void emaReflectsRecentTrendWithinFewSamples() {
        FallbackLatencyTracker tracker = new FallbackLatencyTracker();
        tracker.record("m", 100);
        assertThat(tracker.emaMs("m")).isEqualTo(100.0);
        for (int i = 0; i < 7; i++) {
            tracker.record("m", 1000); // 慢化趋势（α=0.3：7 样本后 >90% 传导）
        }
        assertThat(tracker.emaMs("m")).isGreaterThan(900.0); // 数次内传导
    }

    @Test
    void unknownModelsStayNeutralViaMedian() {
        FallbackLatencyTracker tracker = new FallbackLatencyTracker();
        tracker.record("fast", 50);
        tracker.record("slow", 500);
        // known 中位数 = 275：未知 newcomer（配置序第三）应排在 slow 前、fast 后
        List<NamedFallbackModel> sorted = tracker.sorted(List.of(
                named("slow"), named("newcomer"), named("fast")));
        assertThat(sorted).extracting(NamedFallbackModel::name)
                .containsExactly("fast", "newcomer", "slow");
    }

    @Test
    void allUnknownKeepsOriginalOrderAndInputUntouched() {
        FallbackLatencyTracker tracker = new FallbackLatencyTracker();
        List<NamedFallbackModel> original = List.of(named("a"), named("b"), named("c"));
        List<NamedFallbackModel> sorted = tracker.sorted(original);
        assertThat(sorted).extracting(NamedFallbackModel::name).containsExactly("a", "b", "c");
        assertThat(original).isSameAs(sorted); // 全未知零排序开销（原引用返回）
    }

    @Test
    void equalEmasKeepConfigOrderStably() {
        FallbackLatencyTracker tracker = new FallbackLatencyTracker();
        tracker.record("a", 100);
        tracker.record("b", 100);
        List<NamedFallbackModel> sorted = tracker.sorted(List.of(named("b"), named("a")));
        assertThat(sorted).extracting(NamedFallbackModel::name).containsExactly("b", "a"); // 并列保原序
    }

    @Test
    void nonPositiveDurationsIgnored() {
        FallbackLatencyTracker tracker = new FallbackLatencyTracker();
        tracker.record("m", 0);
        tracker.record("m", -5);
        assertThat(tracker.emaMs("m")).isNull();
    }

    @Test
    void chainWithoutTrackerKeepsStaticOrder() {
        FallbackChain chain = new FallbackChain(
                List.of(named("slow"), named("fast")),
                new io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties.Fallback(
                        List.of("slow", "fast"), null));
        assertThat(chain.models()).extracting(NamedFallbackModel::name)
                .containsExactly("slow", "fast"); // 默认关：静态配置序
        assertThat(chain.latencyTracker()).isNull();
    }

    @Test
    void chainWithTrackerSortsModelsView() {
        FallbackLatencyTracker tracker = new FallbackLatencyTracker();
        tracker.record("slow", 900);
        tracker.record("fast", 10);
        FallbackChain chain = new FallbackChain(
                List.of(named("slow"), named("fast")),
                new io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties.Fallback(
                        List.of("slow", "fast"), null, null, null, true),
                tracker);
        assertThat(chain.models()).extracting(NamedFallbackModel::name)
                .containsExactly("fast", "slow"); // EMA 升序视图（配置序不变、视图变）
        assertThat(chain.latencyTracker()).isSameAs(tracker);
    }
}
