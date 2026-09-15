package io.github.chyuan_cuihongyuan.buzhou.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1732 / T2666：SpanAttributeBudget 直测——超限计数/极值/自定义阈值。
 */
class SpanAttributeBudgetTest {

    @Test
    void overLimitSpansAreCounted() {
        var budget = new SpanAttributeBudget();
        budget.record("ok-span", 10, 500);
        budget.record("fat-span", 200, 500);
        budget.record("heavy-span", 10, 99999);
        var census = budget.census();
        assertThat(census.spans()).isEqualTo(3);
        assertThat(census.overAttrLimit()).isEqualTo(1);
        assertThat(census.overByteLimit()).isEqualTo(1);
        assertThat(census.worstAttrs()).isEqualTo(200);
        assertThat(census.worstBytes()).isEqualTo(99999);
    }

    @Test
    void negativeBytesIgnoredInWorst() {
        var budget = new SpanAttributeBudget();
        budget.record("x", 1, -5);
        assertThat(budget.census().worstBytes()).isZero();
    }

    @Test
    void customThresholds() {
        var budget = new SpanAttributeBudget(4, 100);
        budget.record("a", 5, 10);
        budget.record("b", 2, 200);
        var census = budget.census();
        assertThat(census.overAttrLimit()).isEqualTo(1);
        assertThat(census.overByteLimit()).isEqualTo(1);
    }
}
