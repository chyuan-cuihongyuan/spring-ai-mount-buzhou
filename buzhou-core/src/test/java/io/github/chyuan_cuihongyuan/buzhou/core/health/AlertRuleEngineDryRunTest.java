package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 告警规则 dry-run 测试（spec 716 / T983–T984 / impl 519）：三分类正确 +
 * 三不承诺（状态机/通知/指标零副作用）+ 实弹零污染。
 */
class AlertRuleEngineDryRunTest {

    private static final Instant T0 = Instant.parse("2026-09-13T00:00:00Z");

    private static final class StubHealth implements BuzhouHealth {
        private final String mechanism;
        private volatile Status status = Status.UP;

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

    private static Map<String, BuzhouHealth> sourceOf(BuzhouHealth... healths) {
        Map<String, BuzhouHealth> map = new LinkedHashMap<>();
        for (BuzhouHealth health : healths) {
            map.put(health.mechanism(), health);
        }
        return map;
    }

    static final class CapturingMetrics implements BuzhouMetrics {
        final ConcurrentLinkedQueue<String> events = new ConcurrentLinkedQueue<>();

        @Override
        public void counter(String name, long delta, String... tagKeyValue) {
            events.add(name);
        }

        @Override
        public void timer(String name, Duration duration, String... tagKeyValue) {
            events.add(name);
        }
    }

    private final CapturingMetrics metrics = new CapturingMetrics();
    private final List<AlertRuleEngine.AlertFiring> notified = new java.util.concurrent.CopyOnWriteArrayList<>();

    @AfterEach
    void tearDown() {
        BuzhouMetricsHolder.reset();
    }

    @Test
    void wouldFireReportedWithZeroSideEffects() {
        BuzhouMetricsHolder.install(metrics);
        StubHealth memory = new StubHealth("memory");
        memory.status = BuzhouHealth.Status.DOWN;
        AlertRuleEngine engine = new AlertRuleEngine(
                List.of(new AlertRuleEngine.AlertRule("mem-down", "memory", null)),
                () -> sourceOf(memory));
        engine.onAlert(notified::add);

        AlertRuleEngine.DryRunReport report = engine.dryRun(T0);

        assertThat(report.wouldFire()).hasSize(1);
        assertThat(report.wouldFire().get(0).ruleName()).isEqualTo("mem-down");
        assertThat(report.wouldRecover()).isEmpty();
        assertThat(report.pending()).isEmpty();
        // 三不承诺
        assertThat(engine.firingView()).isEmpty();                       // 状态机未动
        assertThat(notified).isEmpty();                                  // 未通知
        assertThat(metrics.events).isEmpty();                            // 未发指标
    }

    @Test
    void wouldRecoverForCurrentlyFiringUpRule() {
        StubHealth memory = new StubHealth("memory");
        AlertRuleEngine engine = new AlertRuleEngine(
                List.of(new AlertRuleEngine.AlertRule("mem-down", "memory", null)),
                () -> sourceOf(memory));
        engine.onAlert(notified::add);
        memory.status = BuzhouHealth.Status.DOWN;
        engine.evaluate(T0);                                             // 实弹触发
        memory.status = BuzhouHealth.Status.UP;                          // 已恢复但未评估

        AlertRuleEngine.DryRunReport report = engine.dryRun(T0.plusSeconds(1));

        assertThat(report.wouldRecover()).hasSize(1);
        assertThat(report.wouldRecover().get(0).recovered()).isTrue();
        assertThat(report.wouldFire()).isEmpty();
        // 实弹状态机未被 dry-run 推进：evaluate 才真正发恢复
        assertThat(notified).hasSize(1);                                 // 只有最初的触发
        engine.evaluate(T0.plusSeconds(2));
        assertThat(notified).hasSize(2);                                 // 恢复由实弹发出
        assertThat(notified.get(1).recovered()).isTrue();
    }

    @Test
    void pendingReportsRemainingWindow() {
        StubHealth memory = new StubHealth("memory");
        memory.status = BuzhouHealth.Status.DOWN;
        AlertRuleEngine engine = new AlertRuleEngine(
                List.of(new AlertRuleEngine.AlertRule("mem-down", "memory",
                        Duration.ofMinutes(5))),
                () -> sourceOf(memory));

        AlertRuleEngine.DryRunReport report = engine.dryRun(T0.plusSeconds(60));

        assertThat(report.wouldFire()).isEmpty();
        assertThat(report.pending()).hasSize(1);
        // 从未实弹评估——downSince 缺席，downFor 从推演时刻起算（诚实口径：引擎只知自己观察过的 DOWN 起点）
        assertThat(report.pending().get(0).downFor()).isEqualTo(Duration.ZERO);
        assertThat(report.pending().get(0).remaining()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void dryRunDoesNotPolluteLiveSemantics() {
        StubHealth memory = new StubHealth("memory");
        memory.status = BuzhouHealth.Status.DOWN;
        AlertRuleEngine engine = new AlertRuleEngine(
                List.of(new AlertRuleEngine.AlertRule("mem-down", "memory", null)),
                () -> sourceOf(memory));
        engine.onAlert(notified::add);

        engine.dryRun(T0);
        engine.dryRun(T0);
        engine.evaluate(T0); // 实弹首评——若 dryRun 污染状态，此处不会触发

        assertThat(notified).hasSize(1);
        assertThat(notified.get(0).recovered()).isFalse();
    }
}
