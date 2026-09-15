package io.github.chyuan_cuihongyuan.buzhou.guard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1741 / T2684：InjectionCalibrationProbe 直测——四象限归账/三率哨兵。
 */
class InjectionCalibrationProbeTest {

    @Test
    void emptyCarriesSentinels() {
        var probe = new InjectionCalibrationProbe();
        var report = probe.report();
        assertThat(report.precision()).isEqualTo(-1d);
        assertThat(report.recall()).isEqualTo(-1d);
        assertThat(report.accuracy()).isEqualTo(-1d);
    }

    @Test
    void confusionMatrixTallies() {
        var probe = new InjectionCalibrationProbe();
        probe.record(true, true);
        probe.record(true, true);
        probe.record(false, false);
        probe.record(true, false);
        probe.record(false, true);
        var report = probe.report();
        assertThat(report.samples()).isEqualTo(5);
        assertThat(report.tp()).isEqualTo(2);
        assertThat(report.fp()).isEqualTo(1);
        assertThat(report.fn()).isEqualTo(1);
        assertThat(report.tn()).isEqualTo(1);
        assertThat(report.precision()).isCloseTo(2d / 3d, within(1e-9));
        assertThat(report.recall()).isCloseTo(2d / 3d, within(1e-9));
        assertThat(report.accuracy()).isCloseTo(3d / 5d, within(1e-9));
    }

    @Test
    void resetForTest() {
        var probe = new InjectionCalibrationProbe();
        probe.record(true, true);
        probe.resetForTest();
        assertThat(probe.report().samples()).isZero();
    }
}
