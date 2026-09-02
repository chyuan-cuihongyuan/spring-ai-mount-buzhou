package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.runaway.ToolLoopBreakerHook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 327 / impl-350：工具循环断路装配回归——window 配置即 bean / 未配
 * 不装 / window=1 红。
 */
class ToolLoopAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void assemblesWhenWindowConfigured() {
        runner.withPropertyValues("buzhou.runaway.tool-loop.window=3").run(context ->
                assertThat(context).hasBean("buzhouToolLoopBreakerHook"));
    }

    @Test
    void staysOffWhenWindowUnconfigured() {
        runner.run(context ->
                assertThat(context).doesNotHaveBean(ToolLoopBreakerHook.class));
    }

    @Test
    void rejectsInvalidWindow() {
        runner.withPropertyValues("buzhou.runaway.tool-loop.window=1").run(context ->
                assertThat(context).hasFailed());
    }
}
