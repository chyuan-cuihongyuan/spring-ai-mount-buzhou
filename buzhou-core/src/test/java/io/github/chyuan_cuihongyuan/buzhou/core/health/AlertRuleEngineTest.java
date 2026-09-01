package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 312 / impl-335：健康告警规则回归——DOWN 满窗触发 + UP 恢复双向 /
 * for 窗内恢复（flap）不触发 / UNKNOWN 不触发 / 引用缺失机制 fail-fast。
 */
class AlertRuleEngineTest {

    private static final Instant T0 = Instant.parse("2026-09-01T12:00:00Z");

    /** 可翻转的 stub 健康面。 */
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

    private static Map<String, BuzhouHealth> sourceOf(BuzhouHealth... healths) {
        Map<String, BuzhouHealth> map = new LinkedHashMap<>();
        for (BuzhouHealth health : healths) {
            map.put(health.mechanism(), health);
        }
        return map;
    }

    @Test
    void downSustainedFires_thenUpRecovers_bothNotified() {
        StubHealth memory = new StubHealth();
        AlertRuleEngine engine = new AlertRuleEngine(
                List.of(new AlertRuleEngine.AlertRule("memory-down", "memory", null)),
                () -> sourceOf(memory));
        List<AlertRuleEngine.AlertFiring> fired = new CopyOnWriteArrayList<>();
        engine.onAlert(fired::add);
        engine.validateMechanisms();

        memory.status = BuzhouHealth.Status.DOWN;
        engine.evaluate(T0);
        assertThat(fired).hasSize(1);
        assertThat(fired.get(0).recovered()).isFalse();
        assertThat(fired.get(0).mechanism()).isEqualTo("memory");

        memory.status = BuzhouHealth.Status.UP;
        engine.evaluate(T0.plusSeconds(30));
        assertThat(fired).hasSize(2);
        assertThat(fired.get(1).recovered()).isTrue();
    }

    @Test
    void flapInsideForWindowDoesNotFire() {
        StubHealth memory = new StubHealth();
        AlertRuleEngine engine = new AlertRuleEngine(
                List.of(new AlertRuleEngine.AlertRule("memory-down", "memory", Duration.ofMinutes(2))),
                () -> sourceOf(memory));
        List<AlertRuleEngine.AlertFiring> fired = new CopyOnWriteArrayList<>();
        engine.onAlert(fired::add);
        engine.validateMechanisms();

        memory.status = BuzhouHealth.Status.DOWN;
        engine.evaluate(T0); // 开始计时
        engine.evaluate(T0.plusSeconds(60)); // 1 分钟仍 DOWN——for 未满
        memory.status = BuzhouHealth.Status.UP;
        engine.evaluate(T0.plusSeconds(90)); // 窗内恢复——flap 吸收
        assertThat(fired).isEmpty();

        memory.status = BuzhouHealth.Status.DOWN;
        engine.evaluate(T0.plusSeconds(120));
        engine.evaluate(T0.plusSeconds(300)); // 持续满 2 分钟
        assertThat(fired).hasSize(1);
        assertThat(fired.get(0).recovered()).isFalse();
    }

    @Test
    void unknownStatusDoesNotFire() {
        StubHealth memory = new StubHealth();
        AlertRuleEngine engine = new AlertRuleEngine(
                List.of(new AlertRuleEngine.AlertRule("memory-down", "memory", null)),
                () -> sourceOf(memory));
        List<AlertRuleEngine.AlertFiring> fired = new CopyOnWriteArrayList<>();
        engine.onAlert(fired::add);
        engine.validateMechanisms();

        memory.status = BuzhouHealth.Status.UNKNOWN; // 未启用 ≠ 失能
        engine.evaluate(T0);
        engine.evaluate(T0.plusSeconds(60));
        assertThat(fired).isEmpty();
    }

    @Test
    void validateFailsFastOnUnknownMechanism() {
        AlertRuleEngine engine = new AlertRuleEngine(
                List.of(new AlertRuleEngine.AlertRule("ghost-rule", "ghost", null)),
                () -> sourceOf(new StubHealth()));
        assertThatThrownBy(engine::validateMechanisms)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost");
    }
}
