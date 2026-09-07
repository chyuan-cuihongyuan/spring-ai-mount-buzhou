package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.ChaosMonkeyHook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 322 / impl-345：混沌注入 yml 装配回归——enabled=true 装配 hook（BuzhouHook
 * 自动收集）/ 默认不装 / 概率越界启动红 / include 清单绑定。
 */
class ChaosAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void assemblesWhenEnabled() {
        runner.withPropertyValues(
                "buzhou.chaos.enabled=true",
                "buzhou.chaos.exception-percent=5",
                "buzhou.chaos.tools=weather,search").run(context -> {
            assertThat(context).hasBean("buzhouChaosMonkeyHook");
            ChaosMonkeyHook hook = context.getBean(ChaosMonkeyHook.class);
            assertThat(hook.isEnabled()).isTrue();
            assertThat(hook.faultInjectedCount()).isZero(); // 装配即待命，未袭
        });
    }

    @Test
    void staysOffByDefault() {
        runner.withPropertyValues("buzhou.chaos.exception-percent=5").run(context ->
                assertThat(context).doesNotHaveBean(ChaosMonkeyHook.class));
    }

    @Test
    void rejectsOutOfRangePercent() {
        runner.withPropertyValues(
                "buzhou.chaos.enabled=true",
                "buzhou.chaos.latency-percent=101").run(context ->
                assertThat(context).hasFailed());
        runner.withPropertyValues(
                "buzhou.chaos.enabled=true",
                "buzhou.chaos.latency-millis=-1").run(context ->
                assertThat(context).hasFailed());
    }
}
