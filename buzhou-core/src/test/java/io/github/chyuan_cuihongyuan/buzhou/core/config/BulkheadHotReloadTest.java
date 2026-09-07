package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.AgentBulkhead;
import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BulkheadHotReload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 320 / impl-343：舱容量热重载装配回归——舱开即监听 / 事件 + PropertySource
 * 改 → limitOf 生效 / 舱未开不装配。舱装配会 install 全局——@AfterEach 清理。
 */
class BulkheadHotReloadTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @AfterEach
    void cleanup() {
        AgentBulkhead.install(null);
    }

    @Test
    void listenerAssemblesWithBulkhead() {
        runner.withPropertyValues(
                "buzhou.bulkhead.enabled=true",
                "buzhou.bulkhead.agents.a=1").run(context -> {
            assertThat(context).hasBean("buzhouBulkheadHotReload");
            assertThat(context.getBean(BulkheadHotReload.class).reloadCount()).isZero();
            assertThat(context.getBean(AgentBulkhead.class).limitOf("a")).isEqualTo(1);
        });
    }

    @Test
    void reloadAppliesNewLimitsFromEnvironment() {
        runner.withPropertyValues(
                "buzhou.bulkhead.enabled=true",
                "buzhou.bulkhead.agents.a=1").run(context -> {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "hot", Map.of("buzhou.bulkhead.agents.a", 5)));
            context.publishEvent(new BuzhouConfigRefreshEvent(this));
            assertThat(context.getBean(AgentBulkhead.class).limitOf("a"))
                    .as("事件后重读 yml——容量热生效").isEqualTo(5);
            assertThat(context.getBean(BulkheadHotReload.class).reloadCount()).isEqualTo(1);
        });
    }

    @Test
    void noListenerWithoutBulkhead() {
        runner.withPropertyValues("buzhou.bulkhead.agents.a=1").run(context ->
                assertThat(context).doesNotHaveBean(BulkheadHotReload.class));
    }
}
