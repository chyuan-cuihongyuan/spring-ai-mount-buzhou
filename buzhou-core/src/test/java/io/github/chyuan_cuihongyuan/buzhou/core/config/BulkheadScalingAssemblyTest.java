package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.AgentBulkhead;
import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BulkheadScalingAdvisor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 319 / impl-342：舱压伸缩建议 yml 装配回归——舱开 + threshold 配置即装配 /
 * 舱未开（NullBean）不装配 / threshold 未配不装配 / 越界值启动红 / max-multiplier
 * 绑定与默认。舱装配会 install 全局——@AfterEach 清理（跨上下文静态隔离）。
 */
class BulkheadScalingAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @AfterEach
    void cleanup() {
        AgentBulkhead.install(null);
    }

    @Test
    void assemblesWhenBulkheadEnabledAndThresholdConfigured() {
        runner.withPropertyValues(
                "buzhou.bulkhead.enabled=true",
                "buzhou.bulkhead.agents.a=1",
                "buzhou.bulkhead.scaling.scale-up-threshold=5").run(context -> {
            assertThat(context).hasBean("buzhouBulkheadScalingAdvisor");
            BulkheadScalingAdvisor advisor = context.getBean(BulkheadScalingAdvisor.class);
            assertThat(advisor.scaleUpThreshold()).isEqualTo(5);
            assertThat(advisor.maxMultiplier())
                    .as("max-multiplier 未配 = 默认 3").isEqualTo(3);
        });
    }

    @Test
    void staysOffWithoutBulkheadEnabled() {
        runner.withPropertyValues("buzhou.bulkhead.scaling.scale-up-threshold=5")
                .run(context -> assertThat(context)
                        .as("舱未开：NOOP 舱拒绝恒 0 建议恒 1 无意义——NullBean 不装配")
                        .doesNotHaveBean(BulkheadScalingAdvisor.class));
    }

    @Test
    void staysOffWhenThresholdUnconfigured() {
        runner.withPropertyValues(
                "buzhou.bulkhead.enabled=true",
                "buzhou.bulkhead.agents.a=1").run(context ->
                assertThat(context).doesNotHaveBean(BulkheadScalingAdvisor.class));
    }

    @Test
    void rejectsOutOfRangeValues() {
        runner.withPropertyValues(
                "buzhou.bulkhead.enabled=true",
                "buzhou.bulkhead.scaling.scale-up-threshold=0").run(context ->
                assertThat(context).hasFailed());
        runner.withPropertyValues(
                "buzhou.bulkhead.scaling.scale-up-threshold=2",
                "buzhou.bulkhead.scaling.max-multiplier=0").run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    void bindsMaxMultiplier() {
        runner.withPropertyValues(
                "buzhou.bulkhead.enabled=true",
                "buzhou.bulkhead.agents.a=1",
                "buzhou.bulkhead.scaling.scale-up-threshold=2",
                "buzhou.bulkhead.scaling.max-multiplier=5").run(context ->
                assertThat(context.getBean(BulkheadScalingAdvisor.class).maxMultiplier())
                        .isEqualTo(5));
    }
}
