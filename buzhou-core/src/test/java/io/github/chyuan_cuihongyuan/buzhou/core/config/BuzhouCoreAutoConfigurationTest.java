package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内核自装配测试（ticket 22）：内存 store 默认装配、AgentRuntime 收集 RuntimeConfig bean 合成。
 */
class BuzhouCoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class))
            .withBean(ScriptedChatModel.class, ScriptedChatModel::new);

    @Test
    void memoryStoresAndAgentRuntimeAssembleByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(BuzhouStores.class);
            assertThat(ctx).hasSingleBean(AgentRuntime.class);
            // spawn 端到端走通装配链（HookChain / ChatClient 构建 / 租约）
            AgentRuntime runtime = ctx.getBean(AgentRuntime.class);
            runtime.spawn("app", "agent", "sid-core-1").close();
        });
    }

    @Test
    void collectsRuntimeConfigBeansIntoAgentRuntime() {
        runner.withBean(RuntimeConfig.class, () -> RuntimeConfig.defaults())
                .run(ctx -> {
                    assertThat(ctx).getBeans(RuntimeConfig.class).hasSize(1);
                    ctx.getBean(AgentRuntime.class).spawn("app", "agent", "sid-core-2").close();
                });
    }
}
