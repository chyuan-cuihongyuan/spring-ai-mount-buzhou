package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolResultLimiter;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * effort #6/#7 新能力 Spring 装配面（T124 / impl-99）：webhook url→forwarder 装配
 * （默认关）、SessionIndexStore bean→限幅/索引 bean 共存、tools 限幅属性→Holder 生效、
 * 导出扩展 bean 注入 runtime。
 */
class Effort6CapabilitiesAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    /** 默认（无 webhook url）：forwarder 不装配（零开销）；限幅器 Holder 仍是默认档。 */
    @Test
    void defaultsAssembleNothingOptional() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(WebhookEventForwarder.class);
            assertThat(context).hasSingleBean(ToolResultLimiter.class);
            assertThat(context.getBean(ToolResultLimiter.class).limitFor("run_query"))
                    .isEqualTo(ToolResultLimiter.DEFAULT_LIMIT_CHARS);
            assertThat(context.getBean(ToolResultLimiter.class).limitFor("read_range"))
                    .isEqualTo(-1); // 默认豁免
        });
    }

    /** webhook url 配置 → forwarder 装配（outbox 走容器 store）。 */
    @Test
    void webhookUrlAssemblesForwarder() {
        runner.withPropertyValues("buzhou.webhook.url=http://127.0.0.1:9/hook")
                .run(context -> assertThat(context).hasSingleBean(WebhookEventForwarder.class));
    }

    /** 限幅属性 → Holder 生效（覆盖默认档 + 豁免叠加）。 */
    @Test
    void resultLimitPropertiesReachHolder() {
        runner.withPropertyValues(
                        "buzhou.tools.result-limit-chars=5000",
                        "buzhou.tools.result-limit-overrides.my_tool=-1")
                .run(context -> {
                    ToolResultLimiter limiter = context.getBean(ToolResultLimiter.class);
                    assertThat(limiter.limitFor("any")).isEqualTo(5000);
                    assertThat(limiter.limitFor("my_tool")).isEqualTo(-1);
                    assertThat(limiter.limitFor("read_range")).isEqualTo(-1); // 默认豁免保留
                });
    }

    /** SessionIndexStore bean → 与 runtime 共存（索引接线由 buzhouAgentRuntime 消费）。 */
    @Test
    void indexStoreBeanCoexists() {
        runner.withUserConfiguration(IndexProvidedConfig.class)
                .run(context -> assertThat(context).hasSingleBean(SessionIndexStore.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class IndexProvidedConfig {
        @Bean
        SessionIndexStore sessionIndexStore() {
            return new InMemorySessionIndexStore();
        }
    }
}
