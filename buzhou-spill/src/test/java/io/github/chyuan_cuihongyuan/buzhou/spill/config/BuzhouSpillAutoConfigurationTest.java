package io.github.chyuan_cuihongyuan.buzhou.spill.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BuzhouSpillAutoConfigurationTest {

    @TempDir
    Path tempDir;

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        BuzhouCoreAutoConfiguration.class, BuzhouSpillAutoConfiguration.class))
                .withBean(ChatModel.class, ScriptedChatModel::new)
                .withPropertyValues("buzhou.spill.root-dir=" + tempDir,
                        "buzhou.spill.sandbox-root=" + tempDir);
    }

    @Test
    void enabledByDefault() {
        runner().run(ctx -> {
            assertThat(ctx).hasSingleBean(SpillModule.class);
            // spill 产出两个 RuntimeConfig（spill + spillGuard）
            assertThat(ctx).getBeans(RuntimeConfig.class).hasSize(2);
        });
    }

    @Test
    void disabledWhenSwitchedOff() {
        runner().withPropertyValues("buzhou.spill.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(SpillModule.class));
    }

    /**
     * impl-30 / spec 13 §core-1：spill lifecycle 装配——phase 声明（core/memory 之后、
     * guard/store 之前停）；本片为占位（spill 无可关闭资源，诚实边界见
     * {@link SpillModuleLifecycle} Javadoc），上下文 stop 触发后 isRunning 翻 false。
     */
    @Test
    void shouldRegisterSpillLifecyclePlaceholder_whenEnabled() {
        runner().run(ctx -> {
            assertThat(ctx).hasSingleBean(SpillModuleLifecycle.class);
            SpillModuleLifecycle lifecycle = ctx.getBean(SpillModuleLifecycle.class);
            assertThat(lifecycle.getPhase()).isEqualTo(BuzhouLifecyclePhases.SPILL);
            assertThat(lifecycle.getPhase()).isLessThan(BuzhouLifecyclePhases.MEMORY);
            assertThat(lifecycle.getPhase()).isGreaterThan(BuzhouLifecyclePhases.GUARD);
            assertThat(lifecycle.isRunning()).isTrue();
        });
    }
}
