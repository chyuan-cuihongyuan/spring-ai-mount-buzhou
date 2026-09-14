package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1532 / T2315：buzhou.core.tool-batch-response-budget 装配链路（spec 1526
 * 装配面补账——IdempotentRetryAssemblyTest 同批）——值声明即 Holder 生效；缺省零
 * 装配；关闭钩子 reset。
 */
class BatchBudgetAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @AfterEach
    void cleanup() {
        BatchResponseBudgetHolder.reset();
    }

    @Test
    void budgetPropertyInstallsHolder() {
        runner.withPropertyValues("buzhou.core.tool-batch-response-budget=5000")
                .run(ctx -> {
                    assertThat(ctx).hasBean("buzhouBatchResponseBudgetAdapter");
                    assertThat(BatchResponseBudgetHolder.current()).isEqualTo(5000);
                });
    }

    @Test
    void zeroBudgetPropertyStillAssemblesWithZeroSemantics() {
        // 显式 0：bean 装配（属性存在即命中条件）但预算语义为关（enable 内 clamp）
        runner.withPropertyValues("buzhou.core.tool-batch-response-budget=0")
                .run(ctx -> {
                    assertThat(ctx).hasBean("buzhouBatchResponseBudgetAdapter");
                    assertThat(BatchResponseBudgetHolder.current()).isZero();
                });
    }

    @Test
    void absentPropertyAssemblesNoAdapterBean() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean("buzhouBatchResponseBudgetAdapter");
            assertThat(BatchResponseBudgetHolder.current()).isZero();
        });
    }
}
