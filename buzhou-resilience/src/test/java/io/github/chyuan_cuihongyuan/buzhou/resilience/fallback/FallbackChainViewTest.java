package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 207 / T582：单窗视图回归——三源字段合成 / 被逐剔除 / 过期未验标注 /
 * 空链 / null 源降级。
 */
class FallbackChainViewTest {

    private static final class FixedClock extends Clock {
        private final Instant now = Instant.now();

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private static NamedFallbackModel model(String name) {
        ChatModel stub = new ChatModel() {
            @Override public org.springframework.ai.chat.model.ChatResponse call(Prompt p) {
                throw new UnsupportedOperationException();
            }
            @Override public Flux<org.springframework.ai.chat.model.ChatResponse> stream(Prompt p) {
                throw new UnsupportedOperationException();
            }
            @Override public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
                return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
            }
        };
        return new NamedFallbackModel(name, stub);
    }

    private static FallbackChain chain(String... names) {
        List<NamedFallbackModel> models = new java.util.ArrayList<>();
        for (String name : names) {
            models.add(model(name));
        }
        return new FallbackChain(models, null);
    }

    @Test
    void mergesThreeSourcesIntoRows() {
        FallbackChain chain = chain("a", "b", "c");
        ModelOutlierEjection ejection = new ModelOutlierEjection(
                new ModelOutlierEjection.Config(1, Duration.ofMinutes(5)), new FixedClock());
        for (int i = 0; i < 1; i++) {
            ejection.recordError("b");
        }
        FallbackDrill drill = new FallbackDrill(new FixedClock());
        drill.register("a", () -> true);
        drill.drill("a");

        FallbackChainView.Report report = FallbackChainView.report(
                chain, ejection, drill, Duration.ofMinutes(10));

        assertThat(report.rows()).extracting(FallbackChainView.Row::name)
                .containsExactly("a", "b", "c");
        FallbackChainView.Row a = report.rows().get(0);
        assertThat(a.position()).isZero();
        assertThat(a.ejected()).isFalse();
        assertThat(a.fresh()).isTrue(); // 演练过且在窗内
        assertThat(a.lastVerifiedAt()).isNotNull();

        FallbackChainView.Row b = report.rows().get(1);
        assertThat(b.ejected()).isTrue(); // 被逐
        assertThat(report.recommendedOrder()).containsExactly("a", "c"); // 剔除被逐
    }

    @Test
    void staleUnverifiedStaysWithAnnotationNotRemoval() {
        FallbackChain chain = chain("old", "new");
        FallbackDrill drill = new FallbackDrill(new FixedClock());
        drill.register("new", () -> true);
        drill.drill("new"); // 只有 new 有验证

        FallbackChainView.Report report = FallbackChainView.report(
                chain, null, drill, Duration.ofMinutes(10));

        FallbackChainView.Row old = report.rows().get(0);
        assertThat(old.fresh()).isFalse(); // 未验证——标注
        assertThat(old.lastVerifiedAt()).isNull();
        assertThat(report.recommendedOrder()).containsExactly("old", "new"); // 保留不剔除
    }

    @Test
    void nullSourcesDegradeGracefully() {
        FallbackChainView.Report report = FallbackChainView.report(
                chain("a"), null, null, Duration.ofMinutes(5));

        FallbackChainView.Row a = report.rows().getFirst();
        assertThat(a.ejected()).isFalse(); // 无驱逐器=未逐
        assertThat(a.fresh()).isFalse();   // 无演练器=未验证
        assertThat(report.recommendedOrder()).containsExactly("a");
    }

    @Test
    void emptyChainIsEmptyReport() {
        FallbackChainView.Report report = FallbackChainView.report(
                chain(), null, null, Duration.ofMinutes(5));
        assertThat(report.rows()).isEmpty();
        assertThat(report.recommendedOrder()).isEmpty();
    }

    @Test
    void nullChainIsEmptyReport() {
        FallbackChainView.Report report = FallbackChainView.report(
                null, null, null, Duration.ofMinutes(5));
        assertThat(report.rows()).isEmpty();
    }
}
