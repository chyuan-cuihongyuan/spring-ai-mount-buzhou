package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 224 / T592：离群驱逐恐慌阈值（Envoy panic threshold 借鉴）——健康候选跌破
 * 占比阈值时忽略驱逐返回全量（可用性优先）；默认 0=关闭零行为变化；向上取整边界。
 */
class ModelOutlierEjectionPanicTest {

    /** 快测配置：2 连错 / 窗口 200ms。 */
    private static ModelOutlierEjection.Config fast(int panicPercent) {
        return new ModelOutlierEjection.Config(2, Duration.ofMillis(200), panicPercent);
    }

    private static void ejectAll(ModelOutlierEjection ejection, String... names) {
        for (String name : names) {
            ejection.recordError(name);
            ejection.recordError(name);
        }
    }

    private static NamedFallbackModel model(String name) {
        ChatModel stub = new ChatModel() {
            @Override
            public org.springframework.ai.chat.model.ChatResponse call(Prompt prompt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Flux<org.springframework.ai.chat.model.ChatResponse> stream(Prompt prompt) {
                throw new UnsupportedOperationException();
            }
        };
        return new NamedFallbackModel(name, stub);
    }

    /** 默认（0=关）：全逐即空池——既有行为零变化。 */
    @Test
    void panicDisabledByDefaultKeepsEmptyPoolWhenAllEjected() {
        ModelOutlierEjection ejection = new ModelOutlierEjection(fast(0), null);
        ejectAll(ejection, "a", "b");

        assertThat(ejection.filter(List.of(model("a"), model("b")))).isEmpty();
    }

    /** 全逐 + panic=100：返回全量候选（可用性优先）。 */
    @Test
    void panicReturnsFullPoolWhenAllEjected() {
        ModelOutlierEjection ejection = new ModelOutlierEjection(fast(100), null);
        ejectAll(ejection, "a", "b");

        List<NamedFallbackModel> candidates = List.of(model("a"), model("b"));
        assertThat(ejection.filter(candidates)).hasSize(2);
    }

    /** 3 选 1 逐 + panic=50：ceil(1.5)=2，健康 2 ≥ 2 → 不恐慌、正常剔除。 */
    @Test
    void noPanicWhenHealthyAtOrAboveThreshold() {
        ModelOutlierEjection ejection = new ModelOutlierEjection(fast(50), null);
        ejectAll(ejection, "a");

        List<NamedFallbackModel> out = ejection.filter(List.of(model("a"), model("b"), model("c")));
        assertThat(out).hasSize(2);
        assertThat(out.stream().map(NamedFallbackModel::name)).containsExactly("b", "c");
    }

    /** 4 选 4 逐 + panic=50：健康 0 < 2 → 恐慌返回全量 + panicActivations 计数可见。 */
    @Test
    void panicWhenHealthyBelowThreshold() {
        ModelOutlierEjection ejection = new ModelOutlierEjection(fast(50), null);
        ejectAll(ejection, "a", "b", "c", "d");

        List<NamedFallbackModel> candidates = List.of(model("a"), model("b"), model("c"), model("d"));
        assertThat(ejection.filter(candidates)).hasSize(4);
        assertThat(ejection.panicActivations()).isEqualTo(1); // spec 633：观测面可读
    }

    /** 恰在阈值（1/2 健康，pct=50）不恐慌：严格低于才触发。 */
    @Test
    void exactThresholdDoesNotPanic() {
        ModelOutlierEjection ejection = new ModelOutlierEjection(fast(50), null);
        ejectAll(ejection, "a");

        List<NamedFallbackModel> out = ejection.filter(List.of(model("a"), model("b")));
        assertThat(out).hasSize(1);
        assertThat(out.get(0).name()).isEqualTo("b");
    }

    /** 空候选：返回空，不触发恐慌路径。 */
    @Test
    void emptyCandidatesStayEmpty() {
        ModelOutlierEjection ejection = new ModelOutlierEjection(fast(100), null);
        assertThat(ejection.filter(List.of())).isEmpty();
    }

    /** percent 边界校验：负数 / >100 拒绝。 */
    @Test
    void configValidatesPercentRange() {
        assertThatThrownBy(() -> fast(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> fast(101)).isInstanceOf(IllegalArgumentException.class);
    }

    /** 两参兼容构造 = panic 关闭；withPanicAll 预设 = 100。 */
    @Test
    void compatConstructorsKeepSemantics() {
        ModelOutlierEjection.Config twoArg = new ModelOutlierEjection.Config(2, Duration.ofMillis(200));
        assertThat(twoArg.panicThresholdPercent()).isZero();
        assertThat(ModelOutlierEjection.Config.withPanicAll(2, Duration.ofMillis(200))
                .panicThresholdPercent()).isEqualTo(100);
    }
}
