package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1738 / T2678：PiiScanLatency 直测——分位账目/哨兵/负值忽略。
 */
class PiiScanLatencyTest {

    @Test
    void percentileContract() {
        var latency = new PiiScanLatency();
        latency.record(10);
        latency.record(20);
        latency.record(30);
        latency.record(40);
        latency.record(500);
        var report = latency.report();
        assertThat(report.samples()).isEqualTo(5);
        assertThat(report.medianMillis()).isEqualTo(30);
        assertThat(report.p95Millis()).isEqualTo(500);
        assertThat(report.maxMillis()).isEqualTo(500);
    }

    @Test
    void evenMedianIsMidAverage() {
        var latency = new PiiScanLatency();
        latency.record(10);
        latency.record(31);
        assertThat(latency.report().medianMillis()).isEqualTo(20);
    }

    @Test
    void emptyAndNegative() {
        var latency = new PiiScanLatency();
        assertThat(latency.report().medianMillis()).isEqualTo(-1L);
        latency.record(-5);
        assertThat(latency.report().samples()).isZero();
    }
}
