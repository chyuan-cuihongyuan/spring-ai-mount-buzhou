package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 347 / impl-370：告警注解随发回归——触发/恢复携带注解 / 旧构造
 * 空 map 兼容 / 面板 rules 段含注解 / yml 绑定。
 */
class AlertAnnotationsTest {

    private static final Instant T0 = Instant.parse("2026-09-04T12:00:00Z");

    private static final class StubHealth implements BuzhouHealth {
        private Status status = Status.UP;

        @Override
        public String mechanism() {
            return "memory";
        }

        @Override
        public Status status() {
            return status;
        }
    }

    @Test
    void annotationsRideFiringAndRecoveryNotifications() {
        StubHealth memory = new StubHealth();
        AlertRuleEngine engine = new AlertRuleEngine(List.of(new AlertRuleEngine.AlertRule(
                "memory-down", "memory", null,
                Map.of("runbook-url", "https://runbook/memory-down", "summary", "记忆机制失守"))),
                () -> Map.of("memory", (BuzhouHealth) memory));
        List<AlertRuleEngine.AlertFiring> fired = new CopyOnWriteArrayList<>();
        engine.onAlert(fired::add);
        engine.validateMechanisms();

        memory.status = BuzhouHealth.Status.DOWN;
        engine.evaluate(T0);
        memory.status = BuzhouHealth.Status.UP;
        engine.evaluate(T0.plusSeconds(30));

        assertThat(fired).hasSize(2);
        assertThat(fired.get(0).annotations())
                .containsEntry("runbook-url", "https://runbook/memory-down")
                .containsEntry("summary", "记忆机制失守"); // 触发携带
        assertThat(fired.get(1).recovered()).isTrue();
        assertThat(fired.get(1).annotations()).containsKey("runbook-url"); // 恢复同样携带
    }

    @Test
    void legacyConstructorsDefaultToEmptyAnnotations() {
        AlertRuleEngine.AlertRule rule = new AlertRuleEngine.AlertRule("r", "memory", null);
        assertThat(rule.annotations()).isEmpty();
        AlertRuleEngine.AlertFiring firing = new AlertRuleEngine.AlertFiring(
                "r", "memory", false, T0, Map.of());
        assertThat(firing.annotations()).isEmpty(); // 330 门等既有构造零改动
    }

    @Test
    void ymlBindsAnnotationsThroughAssembly() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.alert.rules[0].name=memory-down",
                        "buzhou.alert.rules[0].mechanism=memory",
                        "buzhou.alert.rules[0].annotations.runbook-url=https://runbook/m",
                        "buzhou.alert.rules[0].annotations.summary=记忆失守")
                .withBean("memoryHealth", BuzhouHealth.class, AlertAnnotationsTest.StubHealth::new)
                .run(context -> {
                    org.assertj.core.api.Assertions.assertThat(context).hasNotFailed();
                    AlertRuleEngine engine = context.getBean(AlertRuleEngine.class);
                    assertThat(engine.rules().get(0).annotations())
                            .containsEntry("runbook-url", "https://runbook/m")
                            .containsEntry("summary", "记忆失守");
                });
    }

    @Test
    void dashboardRulesSectionCarriesAnnotations() {
        StubHealth memory = new StubHealth();
        AlertRuleEngine engine = new AlertRuleEngine(List.of(new AlertRuleEngine.AlertRule(
                "memory-down", "memory", Duration.ofMinutes(2),
                Map.of("summary", "记忆失守"))),
                () -> Map.of("memory", (BuzhouHealth) memory));
        Map<String, Object> payload = new BuzhouAlertsEndpoint(engine, null).alertsDashboard();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>)
                ((Map<String, Object>) payload.get("engine")).get("rules");
        @SuppressWarnings("unchecked")
        Map<String, String> annotations = (Map<String, String>) rules.get(0).get("annotations");
        assertThat(annotations).containsEntry("summary", "记忆失守");
    }
}
