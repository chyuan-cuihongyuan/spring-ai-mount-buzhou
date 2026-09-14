package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1531 / T2313：buzhou.core.tool-transient-retry 装配链路（spec 1511 的装配
 * 面测试补账——EvalPruneAssemblyTest 先例）——enabled=true 声明即 Holder 兜底生效
 * （RetryPolicy 透传 + transientOnly 白名单档 + overrides 绑定经 ConfigMaps 数字键
 * 归一）；缺省 Holder 恒 null 零变化；关闭钩子清理。
 */
class IdempotentRetryAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @AfterEach
    void cleanup() {
        IdempotentToolRetryHolder.Holder.reset();
    }

    @Test
    void enabledPropertyInstallsHolderWiring() {
        runner.withPropertyValues(
                        "buzhou.core.tool-transient-retry.enabled=true",
                        "buzhou.core.tool-transient-retry.max-attempts=2",
                        "buzhou.core.tool-transient-retry.initial-backoff=250ms",
                        "buzhou.core.tool-transient-retry.idempotent-overrides[0]=custom_tool")
                .run(ctx -> {
                    assertThat(ctx).hasBean("buzhouIdempotentToolRetryAdapter");
                    IdempotentToolRetryHolder wiring = IdempotentToolRetryHolder.Holder.current();
                    assertThat(wiring).isNotNull();
                    assertThat(wiring.policy().maxAttempts()).isEqualTo(2);
                    assertThat(wiring.policy().initialBackoff()).isEqualTo(Duration.ofMillis(250));
                    assertThat(wiring.policy().transientOnly()).isTrue(); // spec 1511 瞬断白名单档
                    // overrides 经 ConfigMaps 数字键归一（spec 1510）绑成 List
                    assertThat(wiring.idempotentOverrides()).containsExactly("custom_tool");
                });
    }

    @Test
    void absentPropertyAssemblesNoAdapterBean() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean("buzhouIdempotentToolRetryAdapter");
            assertThat(IdempotentToolRetryHolder.Holder.current()).isNull(); // 零行为
        });
    }
}
