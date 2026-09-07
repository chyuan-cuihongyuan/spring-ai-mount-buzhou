package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.AlertGate;
import io.github.chyuan_cuihongyuan.buzhou.core.health.AlertRuleEngine;
import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 330 / impl-353：通知策略门 yml 装配回归——两键全空不建门 /
 * 声明即装配且引擎接线 / 无规则时有门无引擎（不炸——门空转等规则）。
 */
class AlertGateAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void noDeclarationsNoGate() {
        runner.run(context ->
                assertThat(context).doesNotHaveBean(AlertGate.class));
    }

    @Test
    void declarationsAssembleGate_andEngineWiresIt() {
        runner.withPropertyValues(
                "buzhou.alert.rules[0].name=store-down",
                "buzhou.alert.rules[0].mechanism=store",
                "buzhou.alert.silences[0].mechanisms[0]=bulkhead",
                "buzhou.alert.silences[0].duration=30m",
                "buzhou.alert.silences[0].comment=计划内热调整",
                "buzhou.alert.inhibit-rules[0].source-mechanism=store",
                "buzhou.alert.inhibit-rules[0].target-mechanism=tools")
                .withBean("storeHealth", BuzhouHealth.class, StubStoreHealth::new)
                .withBean("toolsHealth", BuzhouHealth.class, StubToolsHealth::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(AlertGate.class);
                    AlertGate gate = context.getBean(AlertGate.class);
                    assertThat(gate.activeSilences()).hasSize(1);
                    assertThat(gate.activeSilences().get(0).mechanisms())
                            .containsExactly("bulkhead");
                    assertThat(gate.inhibitRules()).hasSize(1);
                    assertThat(gate.inhibitRules().get(0).targetMechanism())
                            .isEqualTo("tools");
                    // SmartLifecycle start 期机制校验通过（上下文未失败）
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AlertRuleEngine.class);
                });
    }

    @Test
    void gateWithoutRules_gateIdlesWithoutEngine() {
        runner.withPropertyValues(
                "buzhou.alert.inhibit-rules[0].source-mechanism=store",
                "buzhou.alert.inhibit-rules[0].target-mechanism=tools")
                .withBean("storeHealth", BuzhouHealth.class, StubStoreHealth::new)
                .withBean("toolsHealth", BuzhouHealth.class, StubToolsHealth::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(AlertGate.class);
                    assertThat(context).doesNotHaveBean(AlertRuleEngine.class);
                    assertThat(context).hasNotFailed();
                });
    }

    static final class StubStoreHealth implements BuzhouHealth {
        @Override
        public String mechanism() {
            return "store";
        }

        @Override
        public Status status() {
            return Status.UP;
        }
    }

    static final class StubToolsHealth implements BuzhouHealth {
        @Override
        public String mechanism() {
            return "tools";
        }

        @Override
        public Status status() {
            return Status.UP;
        }
    }
}
