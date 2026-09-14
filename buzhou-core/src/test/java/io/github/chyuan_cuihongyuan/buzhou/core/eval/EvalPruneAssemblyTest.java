package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-701 / spec 958：buzhou.eval.prune 装配链路——enabled=true 声明即
 * EvalPrunePolicyHolder 兜底生效；缺省 Holder 恒 null；关闭钩子清理。
 */
class EvalPruneAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @AfterEach
    void cleanup() {
        EvalPrunePolicyHolder.clear();
    }

    @Test
    void enabledPropertyInstallsHolderPolicy() {
        runner.withPropertyValues(
                        "buzhou.eval.prune.enabled=true",
                        "buzhou.eval.prune.min-items=3",
                        "buzhou.eval.prune.fail-rate-threshold=0.4")
                .run(ctx -> {
                    assertThat(ctx).hasBean("buzhouEvalPrunePolicyAdapter");
                    EvalPrunePolicy policy = EvalPrunePolicyHolder.current();
                    assertThat(policy).isNotNull();
                    assertThat(policy.minItems()).isEqualTo(3);
                    assertThat(policy.failRateThreshold()).isEqualTo(0.4);
                });
    }

    @Test
    void absentPropertyAssemblesNoAdapterBean() {
        runner.run(ctx -> {
            // ConditionalOnProperty 未命中——装配 bean 不存在（Holder 恒 null 零变化）
            assertThat(ctx).doesNotHaveBean("buzhouEvalPrunePolicyAdapter");
            assertThat(EvalPrunePolicyHolder.current()).isNull();
        });
    }
}
