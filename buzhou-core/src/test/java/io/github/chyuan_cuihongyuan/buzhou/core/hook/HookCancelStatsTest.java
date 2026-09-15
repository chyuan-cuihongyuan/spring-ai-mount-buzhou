package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1718 / T2638：HookCancelStats 直测——取消占比/哨兵/reset。
 */
class HookCancelStatsTest {

    @Test
    void emptyCarriesSentinel() {
        var stats = new HookCancelStats();
        assertThat(stats.snapshot().cancelRatio()).isEqualTo(-1d);
        assertThat(stats.snapshot().observed()).isZero();
    }

    @Test
    void cancelRatioTallies() {
        var stats = new HookCancelStats();
        stats.record(true);
        stats.record(false);
        stats.record(false);
        var snapshot = stats.snapshot();
        assertThat(snapshot.observed()).isEqualTo(3);
        assertThat(snapshot.cancelledSkipped()).isEqualTo(2);
        assertThat(snapshot.completed()).isEqualTo(1);
        assertThat(snapshot.cancelRatio()).isCloseTo(2d / 3d, within(1e-9));
    }

    @Test
    void resetForTest() {
        var stats = new HookCancelStats();
        stats.record(false);
        stats.resetForTest();
        assertThat(stats.snapshot().observed()).isZero();
    }
}
