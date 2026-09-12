package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 启动装配摘要测试（spec 625 / T900–T901 / impl 478）：opt-in 声明即装、缺省无 bean、
 * summary 面板含缺省默认值（otel/dashboard 关、其余开）与覆盖值。
 */
class BuzhouAssemblyReportTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @Configuration(proxyBeanMethods = false)
    static class Config {
        @Bean
        BuzhouAssemblyReport buzhouAssemblyReport(org.springframework.core.env.Environment env) {
            return new BuzhouAssemblyReport(env);
        }
    }

    /** summary：缺省默认（机制开、otel/dashboard 关、store=memory、model=unknown）。 */
    @Test
    void summaryDefaultsMatchSafeByDefaultMatrix() {
        runner.run(context -> {
            BuzhouAssemblyReport report = context.getBean(BuzhouAssemblyReport.class);
            var summary = BuzhouAssemblyReport.summary(context.getEnvironment());
            assertThat(summary.get("buzhou.resilience.enabled")).isEqualTo("true");
            assertThat(summary.get("buzhou.observe.otel.enabled")).isEqualTo("false");
            assertThat(summary.get("buzhou.observe.dashboard.enabled")).isEqualTo("false");
            assertThat(summary.get("buzhou.store.type")).isEqualTo("memory");
            assertThat(summary.get("buzhou.model-name")).isEqualTo("unknown");
            assertThat(summary).hasSize(BuzhouAssemblyReport.MECHANISM_KEYS.size() + 2);
        });
    }

    /** yml 覆盖进入面板（显式关 resilience、store=redis、模型名）。 */
    @Test
    void overridesSurfaceInSummary() {
        runner.withPropertyValues(
                "buzhou.resilience.enabled=false",
                "buzhou.store.type=redis",
                "buzhou.model-name=gpt-x")
                .run(context -> {
                    var summary = BuzhouAssemblyReport.summary(context.getEnvironment());
                    assertThat(summary.get("buzhou.resilience.enabled")).isEqualTo("false");
                    assertThat(summary.get("buzhou.store.type")).isEqualTo("redis");
                    assertThat(summary.get("buzhou.model-name")).isEqualTo("gpt-x");
                });
    }

    /** opt-in 装配门：无属性无 bean；enabled=true 有 bean（ApplicationReady 监听就位）。 */
    @Test
    void optInGate() {
        new ApplicationContextRunner()
                .withUserConfiguration(BuzhouCoreAutoConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean("buzhouAssemblyReport"));
        new ApplicationContextRunner()
                .withUserConfiguration(BuzhouCoreAutoConfiguration.class)
                .withPropertyValues("buzhou.assembly-report.enabled=true")
                .run(context -> assertThat(context).hasBean("buzhouAssemblyReport"));
    }
}
