package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.DryRunHook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 323 / impl-346：干跑拦截 yml 装配回归——enabled=true 装配（默认全量
 * 拦）/ 默认不装 / tools 清单绑定。
 */
class DryRunAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void assemblesWhenEnabled() {
        runner.withPropertyValues("buzhou.dry-run.enabled=true").run(context -> {
            assertThat(context).hasBean("buzhouDryRunHook");
            DryRunHook hook = context.getBean(DryRunHook.class);
            assertThat(hook.isEnabled()).isTrue();
            assertThat(hook.beforeTool(null)).isEqualTo(io.github.chyuan_cuihongyuan
                    .buzhou.core.hook.HookResult.CONTINUE); // 空上下文防御
        });
    }

    @Test
    void staysOffByDefault() {
        runner.withPropertyValues("buzhou.dry-run.tools=delete_user").run(context ->
                assertThat(context).doesNotHaveBean(DryRunHook.class));
    }

    @Test
    void bindsToolsList() {
        runner.withPropertyValues(
                "buzhou.dry-run.enabled=true",
                "buzhou.dry-run.tools=delete_user,drop_table").run(context -> {
            DryRunHook hook = context.getBean(DryRunHook.class);
            io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment env =
                    new io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment(
                            "s1", "agent",
                            new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory
                                    .InMemorySessionStateStore());
            io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext read =
                    new io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext(
                            env, "tc1", "search", java.util.Map.of());
            assertThat(hook.beforeTool(read))
                    .as("清单外照常真跑").isEqualTo(io.github.chyuan_cuihongyuan
                            .buzhou.core.hook.HookResult.CONTINUE);
            io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext del =
                    new io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext(
                            env, "tc2", "delete_user", java.util.Map.of());
            assertThat(hook.beforeTool(del)).isInstanceOf(
                    io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.Block.class);
        });
    }
}
