package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolKillSwitchHook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 325 / impl-348：紧急停用装配回归——恒装配（空集直通）/ yml 预停用 /
 * 刷新事件整体覆盖（320 同法：addFirst PropertySource + publishEvent）。
 */
class ToolKillSwitchAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void alwaysAssembledWithEmptySetPassthrough() {
        runner.run(context -> {
            assertThat(context).hasBean("buzhouToolKillSwitchHook");
            assertThat(context).hasBean("buzhouToolKillSwitchHotReload");
            assertThat(context.getBean(ToolKillSwitchHook.class).disabled()).isEmpty();
        });
    }

    @Test
    void ymlPreloadsDisabledTools() {
        runner.withPropertyValues(
                "buzhou.tool-kill-switch.tools=bad_tool,legacy_tool").run(context ->
                assertThat(context.getBean(ToolKillSwitchHook.class).disabled())
                        .containsExactlyInAnyOrder("bad_tool", "legacy_tool"));
    }

    @Test
    void refreshEventOverwritesFromEnvironment() {
        runner.run(context -> {
            ToolKillSwitchHook hook = context.getBean(ToolKillSwitchHook.class);
            hook.disableTools(java.util.Set.of("runtime-emergency")); // 应急改动
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "hot", Map.of("buzhou.tool-kill-switch.tools",
                            List.of("yml-tool"))));
            context.publishEvent(new BuzhouConfigRefreshEvent(this));
            assertThat(hook.disabled())
                    .as("刷新后 yml 是事实源——整体覆盖应急改动")
                    .containsExactly("yml-tool");
        });
    }
}
