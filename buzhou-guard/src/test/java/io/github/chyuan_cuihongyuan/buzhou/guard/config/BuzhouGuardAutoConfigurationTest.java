package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import io.github.chyuan_cuihongyuan.buzhou.guard.hook.GuardAuthApi;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BuzhouGuardAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    BuzhouCoreAutoConfiguration.class, BuzhouGuardAutoConfiguration.class))
            .withBean(ChatModel.class, ScriptedChatModel::new);

    @Test
    void enabledByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(GuardModule.class);
            assertThat(ctx).hasSingleBean(GuardAuthApi.class);
        });
    }

    @Test
    void disabledWhenSwitchedOff() {
        runner.withPropertyValues("buzhou.guard.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(GuardModule.class));
    }

    /**
     * impl-30 / spec 13 §core-1：guard lifecycle 装配——phase 声明（core/memory/spill
     * 之后、store 之前停）；本片为占位（审计链未在装配面接线、无挂起 flush，诚实边界见
     * {@link GuardModuleLifecycle} Javadoc；flush 钩子属切片 39）。
     */
    @Test
    void shouldRegisterGuardLifecyclePlaceholder_whenEnabled() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(GuardModuleLifecycle.class);
            GuardModuleLifecycle lifecycle = ctx.getBean(GuardModuleLifecycle.class);
            assertThat(lifecycle.getPhase()).isEqualTo(BuzhouLifecyclePhases.GUARD);
            assertThat(lifecycle.getPhase()).isLessThan(BuzhouLifecyclePhases.SPILL);
            assertThat(lifecycle.getPhase()).isGreaterThan(BuzhouLifecyclePhases.STORE);
            assertThat(lifecycle.isRunning()).isTrue();
        });
    }
}
