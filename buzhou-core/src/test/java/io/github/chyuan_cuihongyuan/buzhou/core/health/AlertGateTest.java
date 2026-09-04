package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.health.AlertGate.InhibitRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 330 / impl-353：通知策略门回归——静默窗（匹配/全量/惰性过期/撤销/
 * 运行时按钮 + 触发恢复同吞不吞状态）与抑制规则（根因遮蔽衍生、源恢复后
 * 目标可再通知、自抑拒绝）；吞必有痕（计数器）。
 */
class AlertGateTest {

    private static final Instant T0 = Instant.parse("2026-09-04T12:00:00Z");

    private final List<String[]> counters = new CopyOnWriteArrayList<>();

    {
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.install(
                new io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics() {
                    @Override
                    public void counter(String name, long delta, String... tagKeyValue) {
                        counters.add(new String[]{name, String.valueOf(delta),
                                String.join("=", tagKeyValue)});
                    }

                    @Override
                    public void timer(String name, Duration duration, String... tagKeyValue) {
                        // 门只计数不计时
                    }
                });
    }

    @AfterEach
    void resetMetrics() {
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.reset();
    }

    private static AlertRuleEngine.AlertFiring firing(String mechanism) {
        return new AlertRuleEngine.AlertFiring(mechanism + "-down", mechanism, false,
                T0, Map.of());
    }

    private static AlertRuleEngine.AlertFiring recovered(String mechanism) {
        return new AlertRuleEngine.AlertFiring(mechanism + "-down", mechanism, true,
                T0, Map.of());
    }

    private AlertGate gateWith(InhibitRule... rules) {
        return new AlertGate(List.of(rules), null, () -> T0);
    }

    private AlertGate gateWithSilence(AlertGate.Silence silence) {
        return new AlertGate(List.of(), List.of(silence), () -> T0);
    }

    @Test
    void silenceSwallowsFiringAndRecovery_stateStillTracked() {
        AlertGate gate = new AlertGate(List.of(), List.of(AlertGate.ymlSilence(
                "yml-1", Set.of("bulkhead"), Duration.ofMinutes(30), "维护", "ops", T0)),
                () -> T0);
        assertThat(gate.observe(firing("bulkhead"))).isFalse(); // 触发被吞
        assertThat(gate.observe(recovered("bulkhead"))).isFalse(); // 恢复同吞
        assertThat(gate.firingMechanisms()).isEmpty(); // 吞的是通知不是事实
        assertThat(gate.observe(firing("memory"))).isTrue(); // 非匹配机制直通
    }

    @Test
    void silenceWithStarMatchesEverything() {
        AlertGate gate = new AlertGate(List.of(), List.of(AlertGate.ymlSilence(
                "yml-1", Set.of("*"), Duration.ofMinutes(5), "事故静默", "oncall", T0)),
                () -> T0);
        assertThat(gate.observe(firing("memory"))).isFalse();
        assertThat(gate.observe(firing("spill"))).isFalse();
    }

    @Test
    void silenceExpiresLazily_andSwallowedIsNotReplayed() {
        java.util.concurrent.atomic.AtomicReference<Instant> clock =
                new java.util.concurrent.atomic.AtomicReference<>(T0);
        AlertGate gate = new AlertGate(List.of(), List.of(AlertGate.ymlSilence(
                "yml-1", Set.of("memory"), Duration.ofMinutes(10), "", "", T0)),
                clock::get);
        assertThat(gate.observe(firing("memory"))).isFalse(); // 窗内吞
        clock.set(T0.plus(Duration.ofMinutes(11))); // 过期
        assertThat(gate.activeSilences()).isEmpty(); // 惰性清理
        assertThat(gate.observe(firing("memory"))).isTrue(); // 放行
        // 被吞的那枚不重放——只在恢复/再触发时才再有通知机会
    }

    @Test
    void runtimeSilenceButtonTakesEffectImmediately_andCancellable() {
        AlertGate gate = gateWith();
        assertThat(gate.observe(firing("memory"))).isTrue();
        AlertGate.Silence silence = gate.silence(Set.of("memory"),
                Duration.ofMinutes(30), "事故", "oncall");
        assertThat(silence.id()).isEqualTo("silence-1");
        assertThat(gate.observe(firing("memory"))).isFalse(); // 即时生效
        assertThat(gate.activeSilences()).hasSize(1);
        assertThat(gate.cancelSilence(silence.id())).isTrue();
        assertThat(gate.cancelSilence(silence.id())).isFalse(); // 二次撤销诚实失败
        assertThat(gate.observe(firing("memory"))).isTrue();
    }

    @Test
    void inhibitSwallowsTargetWhileSourceFiring() {
        AlertGate gate = gateWith(new InhibitRule("store", "tools"));
        assertThat(gate.observe(firing("store"))).isTrue(); // 根因照常通知
        assertThat(gate.observe(firing("tools"))).isFalse(); // 衍生被抑
        assertThat(gate.firingMechanisms()).containsExactlyInAnyOrder("store", "tools");
    }

    @Test
    void inhibitedTargetNotifiesAgainAfterSourceRecovers() {
        AlertGate gate = gateWith(new InhibitRule("store", "tools"));
        gate.observe(firing("store"));
        assertThat(gate.observe(firing("tools"))).isFalse(); // 抑制中
        assertThat(gate.observe(recovered("store"))).isTrue(); // 根因恢复照常通知
        assertThat(gate.observe(firing("tools"))).isTrue(); // 衍生再触发可再通知
    }

    @Test
    void inhibitIdleWhenSourceNeverFired() {
        AlertGate gate = gateWith(new InhibitRule("store", "tools"));
        assertThat(gate.observe(firing("tools"))).isTrue(); // 源没 firing——不抑
    }

    @Test
    void silenceTakesPriorityOverInhibit_inTrace() {
        AlertGate gate = new AlertGate(
                List.of(new InhibitRule("store", "tools")),
                List.of(AlertGate.ymlSilence("yml-1", Set.of("tools"),
                        Duration.ofMinutes(10), "", "", T0)),
                () -> T0);
        gate.observe(firing("store"));
        assertThat(gate.observe(firing("tools"))).isFalse();
        assertThat(counters).anySatisfy(c -> {
            assertThat(c[0]).isEqualTo("buzhou.alert.silenced"); // 报静默不报抑制
            assertThat(c[2]).contains("mechanism=tools");
        });
    }

    @Test
    void swallowedLeavesCounterTrace() {
        AlertGate gate = gateWith(new InhibitRule("store", "tools"));
        gate.observe(firing("store"));
        gate.observe(firing("tools"));
        assertThat(counters).anySatisfy(c -> {
            assertThat(c[0]).isEqualTo("buzhou.alert.inhibited");
            assertThat(c[2]).contains("mechanism=tools");
        });
    }

    @Test
    void validateMechanisms_failsFastOnUnknownReference() {
        AlertGate gate = gateWith(new InhibitRule("store", "tools"));
        Map<String, BuzhouHealth> healths = new LinkedHashMap<>();
        healths.put("store", null);
        assertThatThrownBy(() -> gate.validateMechanisms(healths))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tools");

        AlertGate starGate = gateWithSilence(AlertGate.ymlSilence(
                "yml-1", Set.of("*"), Duration.ofMinutes(5), "", "", T0));
        starGate.validateMechanisms(healths); // * 恒过
    }

    @Test
    void selfInhibitRejected_bothShapes() {
        assertThatThrownBy(() -> new InhibitRule("store", "store"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("自抑");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new AlertGate.Silence("s1", Set.of(), T0, "", "", T0)); // 空机制集
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> AlertGate.ymlSilence("s1", Set.of("memory"), Duration.ZERO, "", "", T0));
    }

    @Test
    void recoveredUpdatesFiringView() {
        AlertGate gate = gateWith();
        gate.observe(firing("memory"));
        assertThat(gate.firingMechanisms()).containsExactly("memory");
        gate.observe(recovered("memory"));
        assertThat(gate.firingMechanisms()).isEmpty();
    }
}
