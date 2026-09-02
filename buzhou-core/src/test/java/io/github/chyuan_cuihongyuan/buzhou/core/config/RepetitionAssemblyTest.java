package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.runaway.RepetitionDetectorHook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 326 / impl-349：重复检测装配回归——window 配置即 bean / 未配不装 /
 * window=1 启动红。
 */
class RepetitionAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void assemblesWhenWindowConfigured() {
        runner.withPropertyValues(
                "buzhou.runaway.repetition.window=3",
                "buzhou.runaway.repetition.similarity-percent=90",
                "buzhou.runaway.repetition.unstick=true").run(context -> {
            assertThat(context).hasBean("buzhouRepetitionDetectorHook");
            RepetitionDetectorHook hook = context.getBean(RepetitionDetectorHook.class);
            assertThat(hook.currentRun("any")).isZero();
        });
    }

    @Test
    void staysOffWhenWindowUnconfigured() {
        runner.withPropertyValues("buzhou.runaway.repetition.unstick=true")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(RepetitionDetectorHook.class));
    }

    @Test
    void rejectsInvalidWindow() {
        runner.withPropertyValues("buzhou.runaway.repetition.window=1").run(context ->
                assertThat(context).hasFailed());
        runner.withPropertyValues(
                "buzhou.runaway.repetition.window=3",
                "buzhou.runaway.repetition.similarity-percent=0").run(context ->
                assertThat(context).hasFailed());
    }
}
