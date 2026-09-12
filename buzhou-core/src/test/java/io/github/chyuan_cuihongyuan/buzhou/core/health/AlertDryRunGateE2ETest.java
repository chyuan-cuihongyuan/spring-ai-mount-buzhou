package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * dryRun×AlertGate 语义确认（spec 746 / T1041–T1042 / impl 548）：gate 静默吞
 * 实弹通知，但 dryRun.wouldFire 仍如实报告（推演不受门抑制——dry-run=规则引擎
 * 推演，gate=通知通道策略，正交）。
 */
class AlertDryRunGateE2ETest {

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

    @Test
    void gateSuppressesLiveNotificationButDryRunStillReports() {
        StubHealth memory = new StubHealth("memory");
        memory.status = BuzhouHealth.Status.DOWN;
        Map<String, BuzhouHealth> source = new LinkedHashMap<>();
        source.put("memory", memory);

        // 静默窗 10 分钟覆盖全部触发——实弹通知全吞
        AlertGate gate = new AlertGate(List.of(), List.of(AlertGate.ymlSilence(
                "silence-all", java.util.Set.of("memory"), Duration.ofMinutes(10), "", "", T0)),
                () -> T0);
        AlertRuleEngine engine = new AlertRuleEngine(
                List.of(new AlertRuleEngine.AlertRule("mem-down", "memory", null)),
                () -> source, Duration.ofSeconds(30), gate);
        List<AlertRuleEngine.AlertFiring> live = new CopyOnWriteArrayList<>();
        engine.onAlert(live::add);
        engine.evaluate(T0);

        // 实弹：状态机照常 firing，但通知被门吞
        assertThat(engine.firingView()).containsEntry("mem-down", true);
        assertThat(live).isEmpty();

        // dry-run：推演不受门抑制——如实报告实弹「将」触发
        StubHealth db = new StubHealth("db");
        db.status = BuzhouHealth.Status.DOWN;
        AlertRuleEngine engine2 = new AlertRuleEngine(
                List.of(new AlertRuleEngine.AlertRule("db-down", "db", null)),
                () -> {
                    Map<String, BuzhouHealth> s = new LinkedHashMap<>();
                    s.put("db", db);
                    return s;
                }, Duration.ofSeconds(30), gate);
        AlertRuleEngine.DryRunReport report = engine2.dryRun(T0);
        assertThat(report.wouldFire()).hasSize(1); // 门抑制不影响推演
        assertThat(engine2.firingView()).isEmpty(); // 且实弹状态机未被 dry-run 推进
    }
}
