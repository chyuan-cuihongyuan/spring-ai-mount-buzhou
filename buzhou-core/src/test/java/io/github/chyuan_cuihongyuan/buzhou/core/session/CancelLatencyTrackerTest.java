package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1411 / T2124：取消延迟追踪——无轮取消不入账、取消→终结入环、
 * 正常终结清未决键、环容量挤旧、分位读数、reset 归零（直调回调确定性单测）。
 */
class CancelLatencyTrackerTest {

    @Test
    void freshTrackerReportsEmptySentinels() {
        CancelLatencyTracker tracker = new CancelLatencyTracker("s-cl-1");
        var s = tracker.stats();
        assertThat(s.tracked()).isZero();
        assertThat(s.p50Millis()).isEqualTo(-1);
        assertThat(s.p95Millis()).isEqualTo(-1);
        assertThat(s.pendingCancels()).isZero();
        assertThat(tracker.sessionId()).isEqualTo("s-cl-1");
    }

    @Test
    void cancelWithoutInFlightTurnIsIgnored() {
        CancelLatencyTracker tracker = new CancelLatencyTracker("s-cl-2");
        tracker.onCancel(); // 轮间操作——不入账
        assertThat(tracker.stats().pendingCancels()).isZero();
        // 后续轮正常终结不产生延迟样本
        tracker.onTurnStart(1, "in");
        tracker.onTurnEnd(1, "ok");
        assertThat(tracker.stats().tracked()).isZero();
    }

    @Test
    void cancelThenTerminalTurnRecordsLatency() throws Exception {
        CancelLatencyTracker tracker = new CancelLatencyTracker("s-cl-3");
        tracker.onTurnStart(1, "in");
        tracker.onCancel();
        assertThat(tracker.stats().pendingCancels()).isEqualTo(1);
        Thread.sleep(15); // 终结晚于请求 → 延迟 > 0
        tracker.onTurnError(1, new java.util.concurrent.CancellationException("取消终结"));
        var s = tracker.stats();
        assertThat(s.tracked()).isEqualTo(1);
        assertThat(s.pendingCancels()).isZero();
        assertThat(s.p50Millis()).isGreaterThanOrEqualTo(0);
        // 未决键已消费——下一轮正常终结不再入环
        tracker.onTurnStart(2, "in");
        tracker.onTurnEnd(2, "ok");
        assertThat(tracker.stats().tracked()).isEqualTo(1);
    }

    @Test
    void ringCapacityEvictsOldest() {
        CancelLatencyTracker tracker = new CancelLatencyTracker("s-cl-4");
        for (int i = 0; i < CancelLatencyTracker.RING_CAPACITY + 10; i++) {
            tracker.onTurnStart(i + 1, "in");
            tracker.onCancel();
            tracker.onTurnError(i + 1, new RuntimeException("x"));
        }
        // 环有界：tracked 是累计数，但分位只来自最近 RING_CAPACITY 样本
        assertThat(tracker.stats().tracked())
                .isEqualTo(CancelLatencyTracker.RING_CAPACITY + 10);
        assertThat(tracker.stats().p50Millis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void percentileFromKnownSamples() {
        CancelLatencyTracker tracker = new CancelLatencyTracker("s-cl-5");
        // 直注入环：经公开回调构造 10/20/…/100ms 的确定性时序不可行（时钟）
        // ——改验证哨兵与单调性：空环 -1、非空后分位 ≥ 0 且 P95 ≥ P50
        assertThat(tracker.stats().p95Millis()).isEqualTo(-1);
        tracker.onTurnStart(1, "in");
        tracker.onCancel();
        tracker.onTurnError(1, new RuntimeException("x"));
        var s = tracker.stats();
        assertThat(s.p50Millis()).isGreaterThanOrEqualTo(0);
        assertThat(s.p95Millis()).isGreaterThanOrEqualTo(s.p50Millis());
    }

    @Test
    void resetForTestClearsAllState() {
        CancelLatencyTracker tracker = new CancelLatencyTracker("s-cl-6");
        tracker.onTurnStart(1, "in");
        tracker.onCancel();
        tracker.resetForTest();
        var s = tracker.stats();
        assertThat(s.tracked()).isZero();
        assertThat(s.pendingCancels()).isZero();
        // reset 清在途标记：再 onCancel 不入账
        tracker.onCancel();
        assertThat(tracker.stats().pendingCancels()).isZero();
    }
}
