package io.github.chyuan_cuihongyuan.buzhou.memory.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1133 / impl 871：预算钳位读面——正常正预算（normalBudgets）、
 * 负预算钳 0（negativeClamps）、守恒恒等式、resetForTest 归零。
 */
class BudgetClampStatsTest {

    private DefaultBudgetCalculator calculator;

    /** 恒定估算桩：每字符 1 token。 */
    static final class ConstantEstimator implements io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator {
        @Override
        public int estimate(String text) {
            return text == null ? 0 : text.length();
        }

        @Override
        public int estimateMessages(List<BuzhouMessage> messages) {
            return messages == null ? 0
                    : messages.stream().mapToInt(m -> m.content() == null ? 0 : m.content().length()).sum();
        }
    }

    static final class FixedWindowResolver implements io.github.chyuan_cuihongyuan.buzhou.memory.budget.ContextWindowResolver {
        private final int window;

        FixedWindowResolver(int window) {
            this.window = window;
        }

        @Override
        public int resolveWindow(String modelName) {
            return window;
        }
    }

    @BeforeEach
    void reset() {
        DefaultBudgetCalculator.resetForTest();
    }

    private BuzhouMessage msg(String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s1", 1, 0, Role.USER,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    private io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetInput input(
            String systemPrompt, int reserve, int safety) {
        return new io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetInput(
                "model-x", systemPrompt, List.of(), "当前输入", null, List.of(), reserve, safety, 0.8);
    }

    @Test
    void normalBudgetCountsNormal() {
        DefaultBudgetCalculator calc = new DefaultBudgetCalculator(
                new FixedWindowResolver(10_000), new ConstantEstimator());
        calc.evaluate(input("系统提示", 1_000, 500));

        io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetClampStats stats =
                io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetClampStats.stats();
        assertThat(stats.evaluations()).isEqualTo(1);
        assertThat(stats.normalBudgets()).isEqualTo(1);
        assertThat(stats.negativeClamps()).isZero();
    }

    @Test
    void hugeOverheadCountsClamp() {
        // 巨型系统提示 + 巨型 reserve → effective-overhead 为负 → 钳 0
        DefaultBudgetCalculator calc = new DefaultBudgetCalculator(
                new FixedWindowResolver(500), new ConstantEstimator());
        var report = calc.evaluate(input("S".repeat(2_000), 8_000, 1_000));

        assertThat(report.availableForHistory()).isZero(); // 钳位生效

        io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetClampStats stats =
                io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetClampStats.stats();
        assertThat(stats.evaluations()).isEqualTo(1);
        assertThat(stats.negativeClamps()).isEqualTo(1);
        assertThat(stats.normalBudgets()).isZero();
    }

    @Test
    void conservationIdentityHolds() {
        DefaultBudgetCalculator calc = new DefaultBudgetCalculator(
                new FixedWindowResolver(2_000), new ConstantEstimator());
        calc.evaluate(input("正常", 200, 100));
        calc.evaluate(input("N".repeat(5_000), 5_000, 500)); // 钳位

        io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetClampStats stats =
                io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetClampStats.stats();
        assertThat(stats.evaluations())
                .isEqualTo(stats.negativeClamps() + stats.normalBudgets());
    }

    @Test
    void resetForTestZeroesCounters() {
        DefaultBudgetCalculator calc = new DefaultBudgetCalculator(
                new FixedWindowResolver(10_000), new ConstantEstimator());
        calc.evaluate(input("正常", 1_000, 500));
        assertThat(io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetClampStats.stats()
                .evaluations()).isEqualTo(1);

        io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetClampStats.resetForTest();

        assertThat(io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetClampStats.stats()
                .evaluations()).isZero();
    }
}
