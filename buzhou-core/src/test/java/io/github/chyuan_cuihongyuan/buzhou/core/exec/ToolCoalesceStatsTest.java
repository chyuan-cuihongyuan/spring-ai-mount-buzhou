package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1714 / T2630：ToolCoalesceStats 直测——合并组记账/节省比/reset。
 */
class ToolCoalesceStatsTest {

    @Test
    void emptySnapshotCarriesSentinel() {
        var stats = new ToolCoalesceStats();
        assertThat(stats.snapshot().savingRatio()).isEqualTo(-1d);
        assertThat(stats.snapshot().groups()).isZero();
    }

    @Test
    void groupsTallySavings() {
        var stats = new ToolCoalesceStats();
        stats.recordGroup(4);
        stats.recordGroup(2);
        stats.recordGroup(1);
        stats.recordLatencySaved(120);
        stats.recordLatencySaved(-5);
        var snapshot = stats.snapshot();
        assertThat(snapshot.groups()).isEqualTo(2);
        assertThat(snapshot.callsJoined()).isEqualTo(6);
        assertThat(snapshot.savedCalls()).isEqualTo(4);
        assertThat(snapshot.latencySavedMillis()).isEqualTo(120);
        assertThat(snapshot.savingRatio()).isEqualTo(4d / 6d);
    }

    @Test
    void resetForTestZeroes() {
        var stats = new ToolCoalesceStats();
        stats.recordGroup(3);
        stats.resetForTest();
        assertThat(stats.snapshot().callsJoined()).isZero();
        assertThat(stats.snapshot().savingRatio()).isEqualTo(-1d);
    }
}
