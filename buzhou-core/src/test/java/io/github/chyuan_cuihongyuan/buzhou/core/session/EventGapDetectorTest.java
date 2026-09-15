package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1710 / T2622：EventGapDetector 纯函数直测——阈值判定/最大间隙/哨兵。
 */
class EventGapDetectorTest {

    @Test
    void noGapsBelowThreshold() {
        var report = EventGapDetector.analyze(List.of(1000L, 2000L, 3000L), 1500L);
        assertThat(report.events()).isEqualTo(3);
        assertThat(report.gapCount()).isZero();
        assertThat(report.largestGapMillis()).isEqualTo(1000L);
    }

    @Test
    void gapsAboveThresholdAreCounted() {
        var report = EventGapDetector.analyze(List.of(0L, 500L, 5000L, 5400L, 12000L), 1000L);
        assertThat(report.gapCount()).isEqualTo(2);
        assertThat(report.largestGapMillis()).isEqualTo(6600L);
    }

    @Test
    void strictThresholdBoundaryNotCounted() {
        var report = EventGapDetector.analyze(List.of(0L, 1000L), 1000L);
        assertThat(report.gapCount()).isZero();
        assertThat(report.largestGapMillis()).isEqualTo(1000L);
    }

    @Test
    void degenerateSizesCarrySentinel() {
        var empty = EventGapDetector.analyze(List.of(), 500L);
        assertThat(empty.events()).isZero();
        assertThat(empty.largestGapMillis()).isEqualTo(-1L);
        var single = EventGapDetector.analyze(List.of(42L), 500L);
        assertThat(single.gapCount()).isZero();
        assertThat(single.largestGapMillis()).isEqualTo(-1L);
        assertThat(EventGapDetector.analyze(null, 500L).events()).isZero();
    }
}
