package io.github.chyuan_cuihongyuan.buzhou.memory.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * memory 装配开关测试（ticket 22）：默认开产出 RuntimeConfig；关则不装配。
 * impl-30 / spec 13 §core-1 增补：memory SmartLifecycle（sleep-time 调度器 close 接线）。
 */
class BuzhouMemoryAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    BuzhouCoreAutoConfiguration.class, BuzhouMemoryAutoConfiguration.class))
            .withBean(ChatModel.class, ScriptedChatModel::new);

    @Test
    void enabledByDefault() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(RuntimeConfig.class));
    }

    @Test
    void disabledWhenSwitchedOff() {
        runner.withPropertyValues("buzhou.memory.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(RuntimeConfig.class));
    }

    /** impl-30：memory lifecycle 装配——phase 较小（core 之后停）+ sleep-time 调度器已登记待关。 */
    @Test
    void shouldRegisterMemoryLifecycleWithScheduler_whenEnabled() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(MemoryModuleLifecycle.class);
            MemoryModuleLifecycle lifecycle = ctx.getBean(MemoryModuleLifecycle.class);
            assertThat(lifecycle.getPhase()).isEqualTo(BuzhouLifecyclePhases.MEMORY);
            assertThat(lifecycle.isRunning()).isTrue();
            // 主模型在（ScriptedChatModel 兼作摘要模型）→ sleep-time 调度器登记为模块自有资源
            assertThat(lifecycle.managedResourceCount()).isGreaterThanOrEqualTo(1);
        });
    }

    /** impl-30：无摘要模型（仅 memory 微压缩路径）→ 无后台资源，lifecycle 仍做 phase 声明。 */
    @Test
    void shouldRegisterPlaceholderLifecycle_whenNoChatModel() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        BuzhouCoreAutoConfiguration.class, BuzhouMemoryAutoConfiguration.class))
                .withPropertyValues("buzhou.store.type=memory")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(MemoryModuleLifecycle.class);
                    assertThat(ctx.getBean(MemoryModuleLifecycle.class).managedResourceCount())
                            .isZero();
                });
    }
}
