package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 52 §C / T192 / impl-158：评估器 SPI 与内置三评估器。
 */
class BuiltInEvaluatorsTest {

    private static final EvalItem ITEM = new EvalItem("000001", "q", "e", null, null, Instant.now());

    @Test
    void exactComparesTrimmed() {
        assertThat(BuiltInEvaluators.EXACT.evaluate("42 ", "42", ITEM).passed()).isTrue();
        assertThat(BuiltInEvaluators.EXACT.evaluate("43", "42", ITEM).passed()).isFalse();
        assertThat(BuiltInEvaluators.EXACT.evaluate("43", "42", ITEM).detail()).contains("expected=");
    }

    @Test
    void containsMatchesSubstring() {
        assertThat(BuiltInEvaluators.CONTAINS.evaluate("答案是 42，请查收", "42", ITEM).passed()).isTrue();
        assertThat(BuiltInEvaluators.CONTAINS.evaluate("答案是 43", "42", ITEM).passed()).isFalse();
    }

    @Test
    void regexFindsAnywhereAndFailsFastOnBadPattern() {
        Evaluator re = BuiltInEvaluators.regex("\\d{3}-\\d{4}");
        assertThat(re.evaluate("客服电话 010-1234567 谢谢", "\\d{3}-\\d{4}", ITEM).passed()).isTrue();
        assertThat(re.evaluate("无电话", "\\d{3}-\\d{4}", ITEM).passed()).isFalse();
        // 非法正则：构造期 fail-fast（带修法）
        assertThatThrownBy(() -> BuiltInEvaluators.regex("[unclosed"))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("评估正则非法");
    }

    @Test
    void scoreDetailTruncatedToLimit() {
        String huge = "x".repeat(2048);
        EvalScore score = EvalScore.fail("prefix " + huge);
        assertThat(score.detail().length()).isLessThanOrEqualTo(EvalScore.DETAIL_LIMIT + 1);
        assertThat(score.detail()).endsWith("…");
    }

    /** 自定义 Evaluator 直通（SPI 插入口径）。 */
    @Test
    void customEvaluatorPlugsIn() {
        Evaluator always = (actual, expected, item) -> EvalScore.pass("domain-ok:" + item.id());
        assertThat(always.evaluate("any", "any", ITEM).detail()).isEqualTo("domain-ok:000001");
    }
}
