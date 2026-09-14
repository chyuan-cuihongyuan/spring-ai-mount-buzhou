package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1400 / T2102：跨会话轮次并发水位观察者——单测守恒式、峰值水位单调、
 * 双终结防御不为负、reset 归零；并发压测下最终守恒（G r47 压测模式）。
 */
class TurnConcurrencyTrackerTest {

    @Test
    void shouldConserveAcrossOkAndFailedTurns() {
        TurnConcurrencyTracker tracker = new TurnConcurrencyTracker();

        tracker.onTurnStart(1, "a");
        tracker.onTurnStart(2, "b");
        tracker.onTurnEnd(1, "ok-a");
        tracker.onTurnError(2, new IllegalStateException("boom"));

        TurnConcurrencyTracker.Snapshot s = tracker.stats();
        assertThat(s.active()).isZero();
        assertThat(s.peakActive()).isEqualTo(2);
        assertThat(s.started()).isEqualTo(s.okFinished() + s.failed() + s.active());
        assertThat(s.okFinished()).isEqualTo(1);
        assertThat(s.failed()).isEqualTo(1);
    }

    @Test
    void peakShouldBeMonotonicWatermark() {
        TurnConcurrencyTracker tracker = new TurnConcurrencyTracker();
        tracker.onTurnStart(1, "a");
        tracker.onTurnStart(2, "b");
        tracker.onTurnStart(3, "c");
        tracker.onTurnEnd(1, "r1");
        tracker.onTurnEnd(2, "r2");
        tracker.onTurnEnd(3, "r3");
        // 峰值水位不随后续活跃回落而回退
        assertThat(tracker.stats().peakActive()).isEqualTo(3);
        assertThat(tracker.stats().active()).isZero();
    }

    @Test
    void duplicateTerminalCallbacksMustNotDriveActiveNegative() {
        TurnConcurrencyTracker tracker = new TurnConcurrencyTracker();
        tracker.onTurnStart(1, "a");
        tracker.onTurnEnd(1, "r");
        tracker.onTurnEnd(1, "r"); // 重复终结（防御：session 侧 finalized 守卫已兜一层）
        tracker.onTurnError(1, new IllegalStateException("x"));

        assertThat(tracker.stats().active()).isZero();
        assertThat(tracker.stats().okFinished()).isEqualTo(2);
    }

    @Test
    void resetForTestShouldZeroAllCounters() {
        TurnConcurrencyTracker tracker = new TurnConcurrencyTracker();
        tracker.onTurnStart(1, "a");
        tracker.resetForTest();
        assertThat(tracker.stats()).isEqualTo(new TurnConcurrencyTracker.Snapshot(0, 0, 0, 0, 0));
    }

    @Test
    void concurrentStartsAndEndsShouldConserveEventually() throws Exception {
        TurnConcurrencyTracker tracker = new TurnConcurrencyTracker();
        int threads = 8;
        int turnsPerThread = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    ready.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int i = 0; i < turnsPerThread; i++) {
                    tracker.onTurnStart(i, "in");
                    if (i % 3 == 0) {
                        tracker.onTurnError(i, new RuntimeException("t"));
                    } else {
                        tracker.onTurnEnd(i, "r");
                    }
                }
                done.countDown();
            });
        }
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        TurnConcurrencyTracker.Snapshot s = tracker.stats();
        int total = threads * turnsPerThread;
        assertThat(s.started()).isEqualTo(total);
        assertThat(s.active()).isZero();
        assertThat(s.started()).isEqualTo(s.okFinished() + s.failed() + s.active());
        assertThat(s.peakActive()).isGreaterThan(1).isLessThanOrEqualTo(total);
    }
}
