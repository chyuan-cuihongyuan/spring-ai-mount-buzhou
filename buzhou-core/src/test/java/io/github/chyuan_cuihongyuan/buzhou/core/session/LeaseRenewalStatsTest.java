package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1708 / T2618：LeaseRenewalStats 纯函数直测——哨兵/均值/变异系数/偏斜。
 */
class LeaseRenewalStatsTest {

    @Test
    void degenerateSizesCarrySentinel() {
        var empty = LeaseRenewalStats.analyze(List.of());
        assertThat(empty.samples()).isZero();
        assertThat(empty.meanMillis()).isZero();
        assertThat(empty.cv()).isEqualTo(-1d);
        assertThat(empty.maxSkewMillis()).isEqualTo(-1L);

        var single = LeaseRenewalStats.analyze(List.of(5000L));
        assertThat(single.meanMillis()).isEqualTo(5000d);
        assertThat(single.cv()).isEqualTo(-1d);
    }

    @Test
    void steadyCadenceHasNearZeroCv() {
        var report = LeaseRenewalStats.analyze(List.of(5000L, 5000L, 5000L, 5000L));
        assertThat(report.samples()).isEqualTo(4);
        assertThat(report.meanMillis()).isCloseTo(5000d, within(1e-9));
        assertThat(report.cv()).isCloseTo(0d, within(1e-9));
        assertThat(report.maxSkewMillis()).isZero();
    }

    @Test
    void jitteryCadenceHasHighCvAndSkew() {
        var report = LeaseRenewalStats.analyze(List.of(1000L, 9000L, 5000L, 5000L));
        assertThat(report.cv()).isGreaterThan(0.3d);
        assertThat(report.maxSkewMillis()).isEqualTo(8000L);
    }

    @Test
    void negativeSamplesIgnored() {
        var report = LeaseRenewalStats.analyze(List.of(-1L, 5000L, 5000L));
        assertThat(report.samples()).isEqualTo(2);
    }
}
