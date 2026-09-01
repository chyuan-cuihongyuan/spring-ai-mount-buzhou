package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.AlertRuleEngine;
import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 312 / impl-335：告警规则 yml 装配回归——无规则不装配 / 规则绑定 /
 * start 期机制校验（SmartLifecycle 晚于 bean 创建）。
 */
class AlertRuleAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void noRulesNoEngine() {
        runner.run(context ->
                assertThat(context).doesNotHaveBean(AlertRuleEngine.class));
    }

    @Test
    void rulesBindAndEngineAssembles() {
        runner.withPropertyValues(
                "buzhou.alert.interval=15s",
                "buzhou.alert.rules[0].name=memory-down",
                "buzhou.alert.rules[0].mechanism=memory",
                "buzhou.alert.rules[0].for=2m")
                .withBean("memoryHealth", BuzhouHealth.class, StubMemoryHealth::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(AlertRuleEngine.class);
                    BuzhouAlertProperties props = context.getBean(BuzhouAlertProperties.class);
                    assertThat(props.interval()).isEqualTo(Duration.ofSeconds(15));
                    assertThat(props.rules()).hasSize(1);
                    assertThat(props.rules().get(0).mechanism()).isEqualTo("memory");
                    assertThat(props.rules().get(0).forDuration()).isEqualTo(Duration.ofMinutes(2));
                    // SmartLifecycle start 在 ContextRunner close 前—机制在册即校验通过（上下文未失败）
                    assertThat(context).hasNotFailed();
                });
    }

    static final class StubMemoryHealth implements BuzhouHealth {
        @Override
        public String mechanism() {
            return "memory";
        }

        @Override
        public Status status() {
            return Status.UP;
        }
    }
}
