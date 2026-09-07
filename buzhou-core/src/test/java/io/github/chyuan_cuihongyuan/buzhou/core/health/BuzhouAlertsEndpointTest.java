package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 345 / impl-368：告警面板端点回归——齐备全量段 / 部分缺席空段 /
 * firingView 触发后可见。
 */
class BuzhouAlertsEndpointTest {

    private static final Instant T0 = Instant.parse("2026-09-04T12:00:00Z");

    private static final class StubHealth implements BuzhouHealth {
        private final String mechanism;
        private Status status = Status.UP;

        StubHealth(String mechanism) {
            this.mechanism = mechanism;
        }

        @Override
        public String mechanism() {
            return mechanism;
        }

        @Override
        public Status status() {
            return status;
        }
    }

    @Test
    void fullSectionsWhenBothBeansPresent() {
        StubHealth store = new StubHealth("store");
        StubHealth tools = new StubHealth("tools");
        AlertRuleEngine engine = new AlertRuleEngine(List.of(
                new AlertRuleEngine.AlertRule("store-down", "store", null),
                new AlertRuleEngine.AlertRule("tools-down", "tools", Duration.ofMinutes(2))),
                () -> Map.of("store", (BuzhouHealth) store, "tools", tools));
        engine.validateMechanisms();
        AlertGate gate = new AlertGate(
                List.of(new AlertGate.InhibitRule("store", "tools")),
                List.of(AlertGate.ymlSilence("yml-1", Set.of("bulkhead"),
                        Duration.ofMinutes(30), "维护", "ops", T0)),
                () -> T0);

        // store DOWN 持续触发 → firing 视图与面板可见；gate 观察后 tools 触发被抑制
        store.status = BuzhouHealth.Status.DOWN;
        engine.evaluate(T0);
        gate.observe(new AlertRuleEngine.AlertFiring("store-down", "store", false,
                T0, Map.of()));
        tools.status = BuzhouHealth.Status.DOWN;
        engine.evaluate(T0.plusSeconds(120)); // tools 开始计 for=2m 窗
        engine.evaluate(T0.plusSeconds(240)); // 满窗触发
        gate.observe(new AlertRuleEngine.AlertFiring("tools-down", "tools", false,
                T0.plusSeconds(240), Map.of()));

        BuzhouAlertsEndpoint endpoint = new BuzhouAlertsEndpoint(engine, gate);
        Map<String, Object> payload = endpoint.alertsDashboard();

        @SuppressWarnings("unchecked")
        Map<String, Object> engineSection = (Map<String, Object>) payload.get("engine");
        assertThat((List<?>) engineSection.get("rules")).hasSize(2);
        @SuppressWarnings("unchecked")
        Map<String, Boolean> firing = (Map<String, Boolean>) engineSection.get("firing");
        assertThat(firing).containsEntry("store-down", true).containsEntry("tools-down", true);

        @SuppressWarnings("unchecked")
        Map<String, Object> gateSection = (Map<String, Object>) payload.get("gate");
        assertThat((List<?>) gateSection.get("activeSilences")).hasSize(1);
        assertThat((java.util.Collection<String>) gateSection.get("firingMechanisms"))
                .containsExactlyInAnyOrder("store", "tools");
        assertThat((List<?>) gateSection.get("inhibitRules")).hasSize(1);
    }

    @Test
    void absentBeansYieldEmptySections() {
        Map<String, Object> payload = new BuzhouAlertsEndpoint(null, null).alertsDashboard();
        @SuppressWarnings("unchecked")
        Map<String, Object> engineSection = (Map<String, Object>) payload.get("engine");
        @SuppressWarnings("unchecked")
        Map<String, Object> gateSection = (Map<String, Object>) payload.get("gate");
        assertThat((List<?>) engineSection.get("rules")).isEmpty();
        assertThat((Map<?, ?>) engineSection.get("firing")).isEmpty();
        assertThat((List<?>) gateSection.get("activeSilences")).isEmpty();
        assertThat((List<?>) gateSection.get("inhibitRules")).isEmpty(); // 诚实空段
    }

    @Test
    void firingViewReflectsRecovery() {
        StubHealth memory = new StubHealth("memory");
        AlertRuleEngine engine = new AlertRuleEngine(List.of(
                new AlertRuleEngine.AlertRule("memory-down", "memory", null)),
                () -> Map.of("memory", (BuzhouHealth) memory));
        engine.validateMechanisms();
        memory.status = BuzhouHealth.Status.DOWN;
        engine.evaluate(T0);
        assertThat(engine.firingView()).containsEntry("memory-down", true);
        memory.status = BuzhouHealth.Status.UP;
        engine.evaluate(T0.plusSeconds(30));
        assertThat(engine.firingView()).isEmpty(); // 恢复即清
    }
}
