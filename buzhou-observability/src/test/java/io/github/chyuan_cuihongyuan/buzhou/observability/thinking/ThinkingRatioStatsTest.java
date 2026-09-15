package io.github.chyuan_cuihongyuan.buzhou.observability.thinking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1731 / T2664：ThinkingRatioStats 直测——累计/最近占比/哨兵/钳制。
 */
class ThinkingRatioStatsTest {

    @Test
    void emptyCarriesSentinel() {
        var stats = new ThinkingRatioStats();
        var report = stats.report();
        assertThat(report.samples()).isZero();
        assertThat(report.cumulativeRatio()).isEqualTo(-1d);
        assertThat(report.lastRatio()).isEqualTo(-1d);
    }

    @Test
    void cumulativeAndLastRatio() {
        var stats = new ThinkingRatioStats();
        stats.record(200, 1000);
        stats.record(600, 1000);
        var report = stats.report();
        assertThat(report.samples()).isEqualTo(2);
        assertThat(report.totalThinkingChars()).isEqualTo(800);
        assertThat(report.totalChars()).isEqualTo(2000);
        assertThat(report.cumulativeRatio()).isCloseTo(0.4d, within(1e-9));
        assertThat(report.lastRatio()).isCloseTo(0.6d, within(1e-9));
    }

    @Test
    void invalidAndOverflowInputsClamped() {
        var stats = new ThinkingRatioStats();
        stats.record(100, 0);
        stats.record(-50, 100);
        stats.record(500, 100);
        var report = stats.report();
        assertThat(report.samples()).isEqualTo(2);
        assertThat(report.totalThinkingChars()).isEqualTo(100);
        assertThat(report.lastRatio()).isCloseTo(1d, within(1e-9));
    }
}
