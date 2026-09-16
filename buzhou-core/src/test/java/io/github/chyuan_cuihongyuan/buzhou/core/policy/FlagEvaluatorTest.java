package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.FlagEvaluator.FlagDefinition;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.FlagEvaluator.FlagResolution;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.FlagEvaluator.Reason;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2009 / T3120：flag 求值错误语义合同——五态 reason、永不抛出、
 * 谓词抛错兜底、分布计数显形、畸形 fail-fast。
 */
class FlagEvaluatorTest {

    @Test
    void staticFlagShouldResolveStaticVariant() {
        FlagEvaluator evaluator = new FlagEvaluator();
        evaluator.register("exp.readonly", FlagDefinition.staticFlag("on"));
        FlagResolution resolution = evaluator.evaluate("exp.readonly", Map.of());
        assertThat(resolution.variant()).isEqualTo("on");
        assertThat(resolution.reason()).isEqualTo(Reason.STATIC);
    }

    @Test
    void targetingMatchShouldWinOverDefault() {
        FlagEvaluator evaluator = new FlagEvaluator();
        evaluator.register("exp.canary", FlagDefinition.targetedFlag(
                "off", ctx -> "tenant-premium".equals(ctx.get("tenant")), "on"));
        assertThat(evaluator.evaluate("exp.canary", Map.of("tenant", "tenant-premium")).reason())
                .isEqualTo(Reason.TARGETING_MATCH);
        assertThat(evaluator.evaluate("exp.canary", Map.of("tenant", "tenant-free")).variant())
                .isEqualTo("off"); // 未命中回默认变体
        assertThat(evaluator.evaluate("exp.canary", Map.of("tenant", "tenant-free")).reason())
                .isEqualTo(Reason.STATIC);
    }

    @Test
    void unregisteredFlagShouldResolveNotFoundWithNullValue() {
        FlagEvaluator evaluator = new FlagEvaluator();
        FlagResolution resolution = evaluator.evaluate("no.such.flag", Map.of());
        assertThat(resolution.variant()).isNull();
        assertThat(resolution.reason()).isEqualTo(Reason.FLAG_NOT_FOUND);
        assertThat(evaluator.evaluate("no.such.flag", Map.of()).reason())
                .isEqualTo(Reason.FLAG_NOT_FOUND);
    }

    @Test
    void throwingPredicateShouldFallBackToDefaultVariant() {
        FlagEvaluator evaluator = new FlagEvaluator();
        evaluator.register("exp.broken", FlagDefinition.targetedFlag(
                "safe-default", ctx -> {
                    throw new IllegalStateException("targeting 谓词炸了");
                }, "on"));
        FlagResolution resolution = evaluator.evaluate("exp.broken", Map.of());
        assertThat(resolution.variant()).isEqualTo("safe-default"); // 兜底默认
        assertThat(resolution.reason()).isEqualTo(Reason.DEFAULT);
        // 病灶显形：ERROR 与 DEFAULT 同记——分布对账可测
        assertThat(evaluator.reasonCounts().get(Reason.ERROR)).isEqualTo(1L);
        assertThat(evaluator.reasonCounts().get(Reason.DEFAULT)).isEqualTo(1L);
    }

    @Test
    void nullContextShouldEvaluateFine() {
        FlagEvaluator evaluator = new FlagEvaluator();
        evaluator.register("exp.null-ctx", FlagDefinition.targetedFlag(
                "off", ctx -> ctx.containsKey("k"), "on"));
        assertThat(evaluator.evaluate("exp.null-ctx", null).variant()).isEqualTo("off");
    }

    @Test
    void reasonCountsShouldReflectDistribution() {
        FlagEvaluator evaluator = new FlagEvaluator();
        evaluator.register("a", FlagDefinition.staticFlag("v"));
        evaluator.register("b", FlagDefinition.targetedFlag("off", ctx -> true, "on"));
        evaluator.evaluate("a", Map.of());   // STATIC
        evaluator.evaluate("a", Map.of());   // STATIC
        evaluator.evaluate("b", Map.of());   // TARGETING_MATCH
        evaluator.evaluate("c", Map.of());   // FLAG_NOT_FOUND
        Map<Reason, Long> counts = evaluator.reasonCounts();
        assertThat(counts.get(Reason.STATIC)).isEqualTo(2L);
        assertThat(counts.get(Reason.TARGETING_MATCH)).isEqualTo(1L);
        assertThat(counts.get(Reason.FLAG_NOT_FOUND)).isEqualTo(1L);
        assertThat(evaluator.registeredFlags()).containsExactly("a", "b");
    }

    @Test
    void malformedInputsShouldFailFast() {
        FlagEvaluator evaluator = new FlagEvaluator();
        assertThatThrownBy(() -> evaluator.register(null, FlagDefinition.staticFlag("v")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> evaluator.register("  ", FlagDefinition.staticFlag("v")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> evaluator.register("f", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> evaluator.evaluate(null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FlagDefinition("v", null, "targeted"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("成对");
    }
}
