package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1705 / T2612：EvalGateMargin 纯函数直测——边际账目/哨兵/危险带。
 */
class EvalGateMarginTest {

    @Test
    void marginsMinMaxAndBand() {
        var report = EvalGateMargin.analyze(List.of(0.81d, 0.74d, 0.60d), 0.75d);
        assertThat(report.runs()).isEqualTo(3);
        assertThat(report.minMargin()).isCloseTo(0.01d, within(1e-9));
        assertThat(report.maxMargin()).isCloseTo(0.15d, within(1e-9));
        assertThat(report.margins().get(0)).isCloseTo(0.06d, within(1e-9));
        assertThat(report.margins().get(1)).isCloseTo(0.01d, within(1e-9));
        assertThat(report.margins().get(2)).isCloseTo(0.15d, within(1e-9));
        assertThat(report.withinBand(0.02d)).isEqualTo(1);
        assertThat(report.withinBand(1d)).isEqualTo(3);
        assertThat(report.withinBand(0d)).isZero();
    }

    @Test
    void emptyCarriesNegativeSentinel() {
        var report = EvalGateMargin.analyze(List.of(), 0.8d);
        assertThat(report.runs()).isZero();
        assertThat(report.minMargin()).isEqualTo(-1d);
        assertThat(report.maxMargin()).isEqualTo(-1d);
        assertThat(report.withinBand(0.1d)).isZero();
    }

    @Test
    void nullTreatedAsEmptyAndBothSidesOfThreshold() {
        assertThat(EvalGateMargin.analyze(null, 0.5d).runs()).isZero();
        var both = EvalGateMargin.analyze(List.of(0.4d, 0.6d), 0.5d);
        assertThat(both.margins().get(0)).isCloseTo(0.1d, within(1e-9));
        assertThat(both.margins().get(1)).isCloseTo(0.1d, within(1e-9));
    }
}
