package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1810 / T2822：prefetch 信用窗口——满窗拒、确认归还、耗拒计数。 */
class PrefetchCreditWindowTest {

    /** 满窗即拒（耗拒计数）+ 确认归还后续流：在飞上限节流语义。 */
    @Test
    void shouldThrottleAtCapacityAndResumeOnRelease() {
        PrefetchCreditWindow window = new PrefetchCreditWindow(2);
        assertThat(window.tryAcquire()).isTrue();
        assertThat(window.tryAcquire()).isTrue();
        assertThat(window.tryAcquire()).isFalse();
        assertThat(window.tryAcquire()).isFalse();
        assertThat(window.stats().totalExhausted()).isEqualTo(2);
        assertThat(window.stats().utilization()).isEqualTo(1.0d);

        window.release();
        assertThat(window.stats().inFlight()).isEqualTo(1);
        assertThat(window.stats().available()).isEqualTo(1);
        assertThat(window.tryAcquire()).isTrue();
        assertThat(window.stats().totalExhausted()).isEqualTo(2);
    }

    /** 容量 1 的最窄窗：串行取-还语义。 */
    @Test
    void capacityOneWindowIsSerial() {
        PrefetchCreditWindow window = new PrefetchCreditWindow(1);
        assertThat(window.tryAcquire()).isTrue();
        assertThat(window.tryAcquire()).isFalse();
        window.release();
        assertThat(window.tryAcquire()).isTrue();
    }

    /** 初始快照：零在飞、满可用、零耗拒。 */
    @Test
    void initialSnapshotReadsEmpty() {
        PrefetchCreditWindow.Snapshot snapshot = new PrefetchCreditWindow(4).stats();
        assertThat(snapshot.inFlight()).isZero();
        assertThat(snapshot.available()).isEqualTo(4);
        assertThat(snapshot.utilization()).isZero();
        assertThat(snapshot.totalExhausted()).isZero();
    }

    /** 畸形与误用 fail-fast：容量 < 1、空窗 release。 */
    @Test
    void malformedUsageFailsFast() {
        assertThatThrownBy(() -> new PrefetchCreditWindow(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacity 不能小于 1");
        assertThatThrownBy(() -> new PrefetchCreditWindow(2).release())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("release 必须对应已 acquire");
    }
}
