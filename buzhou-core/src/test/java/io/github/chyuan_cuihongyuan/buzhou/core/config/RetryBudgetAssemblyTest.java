package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudgetHolder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 302 / impl-325：重试预算 yml 装配回归——任一键配置即全局生效 / 未配置零装配 /
 * 容器关闭清 holder（跨上下文静态隔离）。
 */
class RetryBudgetAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void shouldStayOff_whenRetryBudgetUnconfigured() {
        runner.run(context -> {
            assertThat(context).hasBean("buzhouRetryBudgetAdapter");
            assertThat(RetryBudgetHolder.current())
                    .as("未配置 retry-budget：holder 保持 null（零行为变化）").isNull();
        });
        assertThat(RetryBudgetHolder.current()).as("上下文关闭后清理").isNull();
    }

    @Test
    void shouldSetHolder_whenAnyKeyConfigured() {
        runner.withPropertyValues("buzhou.backpressure.retry-budget.percent=50").run(context -> {
            assertThat(RetryBudgetHolder.current()).isNotNull();
            assertThat(RetryBudgetHolder.current().balance())
                    .as("min-balance 组内默认 10").isEqualTo(10L);
        });
        assertThat(RetryBudgetHolder.current()).as("上下文关闭后清理").isNull();
    }

    @Test
    void shouldBindBothKeys() {
        runner.withPropertyValues(
                "buzhou.backpressure.retry-budget.percent=5",
                "buzhou.backpressure.retry-budget.min-balance=2").run(context -> {
            BuzhouBackpressureProperties props = context.getBean(BuzhouBackpressureProperties.class);
            assertThat(props.retryBudget().percent()).isEqualTo(5.0);
            assertThat(props.retryBudget().minBalance()).isEqualTo(2L);
            assertThat(RetryBudgetHolder.current().balance()).isEqualTo(2L);
        });
    }

    @Test
    void shouldRejectOutOfRangeValues() {
        runner.withPropertyValues("buzhou.backpressure.retry-budget.percent=0").run(context ->
                assertThat(context).hasFailed());
        runner.withPropertyValues("buzhou.backpressure.retry-budget.min-balance=-1").run(context ->
                assertThat(context).hasFailed());
    }
}
