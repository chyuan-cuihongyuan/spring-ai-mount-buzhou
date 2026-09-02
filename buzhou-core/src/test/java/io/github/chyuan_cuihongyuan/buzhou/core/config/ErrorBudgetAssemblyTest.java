package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget;
import io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudgetHealth;
import io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudgetHook;
import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 321 / impl-344：错误预算 yml 装配回归——slo 配置即三 bean（预算/喂数
 * hook/健康面）/ 未配不装配 / 越界值启动红 / 健康 UNKNOWN→DOWN 迁移（喂败样本）。
 */
class ErrorBudgetAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void assemblesThreeBeansWhenSloConfigured() {
        runner.withPropertyValues(
                "buzhou.error-budget.slo=99.9",
                "buzhou.error-budget.min-samples=2").run(context -> {
            assertThat(context).hasBean("buzhouErrorBudget");
            assertThat(context).hasBean("buzhouErrorBudgetHook");
            assertThat(context).hasBean("buzhouErrorBudgetHealth");
            ErrorBudget budget = context.getBean(ErrorBudget.class);
            assertThat(budget.config().sloPercent()).isEqualTo(99.9);
            assertThat(budget.config().minSamples())
                    .as("显式 min-samples=2 生效").isEqualTo(2);
            assertThat(budget.config().burnRateThreshold())
                    .as("未配 = 默认 2.0").isEqualTo(2.0);
            assertThat(context.getBean(ErrorBudgetHealth.class).status())
                    .as("无样本 = UNKNOWN（未启用 ≠ DOWN）")
                    .isEqualTo(BuzhouHealth.Status.UNKNOWN);
        });
    }

    @Test
    void healthFlipsToDownWhenBreaching() {
        runner.withPropertyValues(
                "buzhou.error-budget.slo=99",
                "buzhou.error-budget.min-samples=2").run(context -> {
            ErrorBudget budget = context.getBean(ErrorBudget.class);
            budget.record("t", false);
            budget.record("t", false); // burn 100 ≥ 2 且样本 2
            assertThat(context.getBean(ErrorBudgetHealth.class).status())
                    .as("燃尽超阈 = SLO 保卫失守 = DOWN（严格语义）")
                    .isEqualTo(BuzhouHealth.Status.DOWN);
            assertThat(context.getBean(ErrorBudgetHealth.class).details())
                    .asString().contains("topBreaching");
        });
    }

    @Test
    void staysOffWhenSloUnconfigured() {
        runner.withPropertyValues("buzhou.error-budget.min-samples=5").run(context -> {
            assertThat(context).doesNotHaveBean(ErrorBudget.class);
            assertThat(context).doesNotHaveBean(ErrorBudgetHook.class);
            assertThat(context).doesNotHaveBean(ErrorBudgetHealth.class);
        });
    }

    @Test
    void rejectsOutOfRangeValues() {
        runner.withPropertyValues("buzhou.error-budget.slo=0").run(context ->
                assertThat(context).hasFailed());
        runner.withPropertyValues("buzhou.error-budget.slo=100").run(context ->
                assertThat(context).hasFailed());
        runner.withPropertyValues(
                "buzhou.error-budget.slo=99",
                "buzhou.error-budget.buckets=1").run(context ->
                assertThat(context).hasFailed());
    }
}
