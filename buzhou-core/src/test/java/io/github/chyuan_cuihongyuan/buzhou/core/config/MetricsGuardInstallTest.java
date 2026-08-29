package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.TagCardinalityGuard;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 160 §B / T513：tag 基数守卫 opt-in 装配红队——默认关 = 裸
 * MicrometerBuzhouMetrics 零变化；开 = Holder 安装的是 TagCardinalityGuard
 * 装饰实例（走真实装配路径断言类型）。
 */
class MetricsGuardInstallTest {

    @AfterEach
    void cleanup() {
        BuzhouMetricsHolder.reset();
    }

    @Configuration(proxyBeanMethods = false)
    static class RegistryConfig {
        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Test
    void defaultOffInstallsPlainMetricsZeroChange() {
        new ApplicationContextRunner()
                .withUserConfiguration(RegistryConfig.class)
                .withUserConfiguration(BuzhouCoreAutoConfiguration.BuzhouMetricsConfiguration.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(
                            io.github.chyuan_cuihongyuan.buzhou.core.metrics
                                    .BuzhouMetricsHolderInstaller.class);
                    assertThat(BuzhouMetricsHolder.metrics())
                            .isNotInstanceOf(TagCardinalityGuard.class);
                });
    }

    @Test
    void optInWrapsWithCardinalityGuard() {
        new ApplicationContextRunner()
                .withUserConfiguration(RegistryConfig.class)
                .withUserConfiguration(BuzhouCoreAutoConfiguration.BuzhouMetricsConfiguration.class)
                .withPropertyValues("buzhou.metrics.cardinality-guard.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(
                            io.github.chyuan_cuihongyuan.buzhou.core.metrics
                                    .BuzhouMetricsHolderInstaller.class);
                    assertThat(BuzhouMetricsHolder.metrics())
                            .isInstanceOf(TagCardinalityGuard.class);
                });
    }
}
