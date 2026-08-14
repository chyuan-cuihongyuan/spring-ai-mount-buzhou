package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealthEndpoint;
import io.github.chyuan_cuihongyuan.buzhou.core.leak.LeakDetectorHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsBinder;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-41 / spec 13 §T66 装配矩阵：有/无 MeterRegistry × 泄漏级别配置——
 * micrometer 在场时 holder 安装 micrometer 实现 + 标准集预注册；不在场时 no-op；
 * actuator 在场时 /buzhou 端点装配；泄漏级别非法值启动即失败。
 */
class BuzhouObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void metricsNoopWithoutMeterRegistry() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(BuzhouMetricsBinder.class);
            // no-op 默认：计数调用零开销不抛
            BuzhouMetricsHolder.metrics().counter("buzhou.probe");
        });
    }

    @Test
    void metricsInstalledAndBoundWithMeterRegistry() {
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new).run(ctx -> {
            assertThat(ctx).hasSingleBean(BuzhouMetricsBinder.class);
            BuzhouMetricsHolder.metrics().counter("buzhou.guard.checks", "outcome", "allowed");
            MeterRegistry registry = ctx.getBean(MeterRegistry.class);
            assertThat(registry.counter("buzhou.guard.checks", "outcome", "allowed").count())
                    .isEqualTo(1.0);
            // 预注册标准集（零值起步）
            assertThat(registry.counter("buzhou.tool.calls", "outcome", "ok").count()).isZero();
        });
    }

    @Test
    void leakDetectorDefaultsToSimpleAndConfigurable() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(ResourceLeakDetector.class);
            assertThat(LeakDetectorHolder.detector().level())
                    .isEqualTo(ResourceLeakDetector.LeakLevel.SIMPLE);
        });
        runner.withPropertyValues("buzhou.leak.level=PARANOID").run(ctx ->
                assertThat(LeakDetectorHolder.detector().level())
                        .isEqualTo(ResourceLeakDetector.LeakLevel.PARANOID));
        runner.withPropertyValues("buzhou.leak.level=nope").run(ctx ->
                assertThat(ctx).hasFailed());
    }

    @Test
    void healthEndpointPresentWithActuator() {
        runner.run(ctx -> {
            // core 测试类路径含 actuator（optional 依赖）→ 端点装配
            assertThat(ctx).hasSingleBean(BuzhouHealthEndpoint.class);
            BuzhouHealthEndpoint endpoint = ctx.getBean(BuzhouHealthEndpoint.class);
            assertThat(endpoint.buzhouSnapshot()).containsKey("mechanisms");
        });
    }
}
