package io.github.chyuan_cuihongyuan.buzhou.memory.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.token.CharHeuristicTokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.core.token.TableContextWindowResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1133 / impl 871：预算钳位读面——正常正预算（normalBudgets）、
 * 负预算钳 0（negativeClamps）、守恒恒等式、resetForTest 归零。
 * 用真类 CharHeuristicTokenEstimator/TableContextWindowResolver（既有测试同款）。
 */
class BudgetClampStatsTest {

    private DefaultBudgetCalculator calc;

    @BeforeEach
    void reset() {
        DefaultBudgetCalculator.resetForTest();
        calc = new DefaultBudgetCalculator(
                new TableContextWindowResolver(Map.of()), new CharHeuristicTokenEstimator());
    }

    private BudgetInput input(String systemPrompt, int reserve, int safety) {
        return new BudgetInput("model-x", systemPrompt, List.of(), "当前输入",
                null, List.of(), reserve, safety, 0.8);
    }

    @Test
    void normalBudgetCountsNormal() {
        calc.evaluate(input("系统提示", 1_000, 500));

        BudgetClampStats stats = DefaultBudgetCalculator.stats();
        assertThat(stats.evaluations()).isEqualTo(1);
        assertThat(stats.normalBudgets()).isEqualTo(1);
        assertThat(stats.negativeClamps()).isZero();
    }

    @Test
    void hugeOverheadCountsClamp() {
        // 巨型系统提示 + 巨型 reserve → effective-overhead 为负 → 钳 0
        var report = calc.evaluate(input("S".repeat(4_000), 9_000, 1_000));
        assertThat(report.availableForHistory()).isZero();

        BudgetClampStats stats = DefaultBudgetCalculator.stats();
        assertThat(stats.evaluations()).isEqualTo(1);
        assertThat(stats.negativeClamps()).isEqualTo(1);
        assertThat(stats.normalBudgets()).isZero();
    }

    @Test
    void conservationIdentityHolds() {
        calc.evaluate(input("正常", 200, 100));
        calc.evaluate(input("N".repeat(6_000), 8_000, 500));

        BudgetClampStats stats = DefaultBudgetCalculator.stats();
        assertThat(stats.evaluations())
                .isEqualTo(stats.negativeClamps() + stats.normalBudgets());
        assertThat(stats.negativeClamps()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        calc.evaluate(input("正常", 1_000, 500));
        assertThat(DefaultBudgetCalculator.stats().evaluations()).isEqualTo(1);

        DefaultBudgetCalculator.resetForTest();

        assertThat(DefaultBudgetCalculator.stats().evaluations()).isZero();
    }
}
