package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1722 / T2646：ReplaySkewStats 直测——偏斜账目/倒挂分离/哨兵。
 */
class ReplaySkewStatsTest {

    @Test
    void positiveSkewsMedianAndMax() {
        var stats = new ReplaySkewStats();
        stats.record(1000L, 3000L);
        stats.record(1000L, 2000L);
        stats.record(1000L, 9000L);
        var report = stats.report();
        assertThat(report.samples()).isEqualTo(3);
        assertThat(report.medianLagMillis()).isEqualTo(2000L);
        assertThat(report.maxLagMillis()).isEqualTo(8000L);
        assertThat(report.negativeSkewCount()).isZero();
    }

    @Test
    void negativeSkewsAreSeparated() {
        var stats = new ReplaySkewStats();
        stats.record(5000L, 4000L);
        stats.record(5000L, 4900L);
        stats.record(1000L, 2000L);
        var report = stats.report();
        assertThat(report.samples()).isEqualTo(3);
        assertThat(report.negativeSkewCount()).isEqualTo(2);
        assertThat(report.medianLagMillis()).isEqualTo(1000L);
        assertThat(report.maxLagMillis()).isEqualTo(1000L);
    }

    @Test
    void emptyCarriesSentinel() {
        var stats = new ReplaySkewStats();
        var report = stats.report();
        assertThat(report.samples()).isZero();
        assertThat(report.medianLagMillis()).isEqualTo(-1L);
        assertThat(report.maxLagMillis()).isEqualTo(-1L);
    }
}
