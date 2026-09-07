package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.ErrorBudgetPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 335 / impl-358：错误预算政策装配回归——enabled=true+slo 配置 →
 * 地板+政策装配 / 缺省零变化 / 有 enabled 无 slo 启动红（盲动拦截）。
 */
class ErrorBudgetFreezeAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void enabledWithBudgetAssemblesFloorAndPolicy() {
        runner.withPropertyValues(
                "buzhou.error-budget.slo=99.9",
                "buzhou.backpressure.error-budget-freeze.enabled=true",
                "buzhou.backpressure.error-budget-freeze.interval=5s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SpawnAdmissionFloor.class);
                    assertThat(context).hasSingleBean(ErrorBudgetPolicy.class);
                    ErrorBudgetPolicy policy = context.getBean(ErrorBudgetPolicy.class);
                    assertThat(policy.view()).containsEntry("interval", "PT5S");
                });
    }

    @Test
    void disabledByDefault_policyAssemblesNothing_floorAlwaysPresent() {
        // spec 342 起：地板槽恒供（cordon 与冻结共用多源槽）——policy 仍条件装配
        runner.withPropertyValues("buzhou.error-budget.slo=99.9")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SpawnAdmissionFloor.class);
                    assertThat(context).doesNotHaveBean(ErrorBudgetPolicy.class);
                });
    }

    @Test
    void enabledWithoutBudgetFailsFast() {
        runner.withPropertyValues("buzhou.backpressure.error-budget-freeze.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }
}
