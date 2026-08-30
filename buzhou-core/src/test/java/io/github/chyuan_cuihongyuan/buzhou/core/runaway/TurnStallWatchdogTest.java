package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 162 §B / T515：停滞巡检犬红队——单轮巡检全集自取（registered 视图）+
 * quiet 降序交付 listener（空表也通知——「跑过无事」是事实）；每轮重复告警
 * （停滞是持续状态，去重归接收端）；周期调度起停 + stop 后不再触发；参数
 * fail-fast。借鉴：K8s liveness probe 周期自检。
 */
class TurnStallWatchdogTest {

    private static final Instant T0 = Instant.parse("2026-08-29T00:00:00Z");

    @Test
    void inspectOnceReportsStalledSortedWithEmptyRunsToo() {
        TurnHeartbeat heartbeat = new TurnHeartbeat();
        heartbeat.register("quiet-long", T0);
        heartbeat.register("quiet-short", T0.plusSeconds(20));
        heartbeat.register("fresh", T0.plusSeconds(59));
        TurnStallWatchdog watchdog = new TurnStallWatchdog(heartbeat,
                Duration.ofSeconds(10), Duration.ofHours(1), false);
        CopyOnWriteArrayList<List<TurnHeartbeat.Stalled>> reports = new CopyOnWriteArrayList<>();
        watchdog.addListener(reports::add);

        watchdog.inspectOnce(); // now = 真实时钟：注册时刻在远古 → 全部停滞
        assertThat(reports).hasSize(1);
        List<TurnHeartbeat.Stalled> stalled = reports.get(0);
        // quiet 降序：停得最久的 quiet-long 居首（排障视线先落最卡处）
        assertThat(stalled).extracting(TurnHeartbeat.Stalled::sessionId)
                .containsExactly("quiet-long", "quiet-short", "fresh");

        heartbeat.clear("quiet-long");
        heartbeat.clear("quiet-short");
        heartbeat.clear("fresh");
        watchdog.inspectOnce(); // 空表也通知（零停滞是事实）
        assertThat(reports).hasSize(2);
        assertThat(reports.get(1)).isEmpty();
    }

    @Test
    void scheduledInspectionFiresRepeatedlyThenStopHalts() throws Exception {
        TurnHeartbeat heartbeat = new TurnHeartbeat();
        heartbeat.register("stuck", Instant.now().minusSeconds(3600));
        TurnStallWatchdog watchdog = new TurnStallWatchdog(heartbeat,
                Duration.ofSeconds(1), Duration.ofMillis(40), true);
        CopyOnWriteArrayList<List<TurnHeartbeat.Stalled>> reports = new CopyOnWriteArrayList<>();
        watchdog.addListener(reports::add);

        watchdog.start();
        assertThat(watchdog.isRunning()).isTrue();
        TimeUnit.MILLISECONDS.sleep(300);
        watchdog.stop();
        int roundsAtStop = reports.size();
        assertThat(roundsAtStop).isGreaterThanOrEqualTo(2);
        // 每轮都报（重复告警语义——停滞是持续状态）
        assertThat(reports).allSatisfy(list ->
                assertThat(list).extracting(TurnHeartbeat.Stalled::sessionId)
                        .contains("stuck"));
        TimeUnit.MILLISECONDS.sleep(150);
        assertThat(reports.size()).isEqualTo(roundsAtStop); // stop 后不再触发
    }

    @Test
    void disabledNeverSchedulesAndArgumentsValidated() {
        TurnHeartbeat heartbeat = new TurnHeartbeat();
        TurnStallWatchdog disabled = new TurnStallWatchdog(heartbeat,
                Duration.ofSeconds(10), Duration.ofHours(1), false);
        disabled.start();
        assertThat(disabled.isRunning()).isFalse(); // 默认不排程
        disabled.close();

        assertThatThrownBy(() -> new TurnStallWatchdog(heartbeat, null,
                Duration.ofHours(1), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TurnStallWatchdog(heartbeat, Duration.ofSeconds(1),
                Duration.ZERO, true))
                .isInstanceOf(IllegalArgumentException.class);
        // null 心跳自持新实例（不 NPE 的宽进面）
        assertThat(new TurnStallWatchdog(null, Duration.ofSeconds(1),
                Duration.ofHours(1), true).heartbeat()).isNotNull();
    }
}
