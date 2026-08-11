package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 韧性层自装配测试：默认开、yml 参数绑定、enabled=false 回退（对齐各模块 {@code *AutoConfigurationTest}）。
 */
class BuzhouResilienceAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouResilienceAutoConfiguration.class));

    @Test
    void enabledByDefaultRegistersCustomizer() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(RuntimeConfig.class);
            assertThat(context.getBean(RuntimeConfig.class).assemblyCustomizers()).hasSize(1);
        });
    }

    @Test
    void disabledDropsCustomizer() {
        // enabled=false 命中 @ConditionalOnProperty 的负向匹配：整个 AutoConfiguration 不装载，
        // 不贡献 RuntimeConfig bean——等同于「不引入本模块」，回退底座原生行为。
        runner.withPropertyValues("buzhou.resilience.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(RuntimeConfig.class);
        });
    }

    @Test
    void ymlOverridesBindToProperties() {
        runner.withPropertyValues(
                "buzhou.resilience.max-attempts=7",
                "buzhou.resilience.initial-backoff=2s",
                "buzhou.resilience.jitter=0").run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.maxAttempts()).isEqualTo(7);
            assertThat(props.initialBackoff()).isEqualTo(Duration.ofSeconds(2));
            assertThat(props.jitter()).isEqualTo(0.0);
        });
    }
}
