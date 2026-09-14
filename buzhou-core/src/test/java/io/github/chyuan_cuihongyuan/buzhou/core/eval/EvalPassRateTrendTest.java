package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1444 / T2190：评估通过率趋势——Theil–Sen 稳健斜率：单调改进/退化、
 * 稳定死区、离群 run 不扭曲方向（中位数抗噪）、样本不足哨兵。
 */
class EvalPassRateTrendTest {

    @Test
    void insufficientSamplesSentinel() {
        assertThat(EvalPassRateTrend.analyze(List.of()).direction())
                .isEqualTo(EvalPassRateTrend.Direction.INSUFFICIENT);
        assertThat(EvalPassRateTrend.analyze(List.of(0.5)).direction())
                .isEqualTo(EvalPassRateTrend.Direction.INSUFFICIENT);
    }

    @Test
    void improvingTrendDetected() {
        var r = EvalPassRateTrend.analyze(List.of(0.5, 0.6, 0.7, 0.8));
        assertThat(r.direction()).isEqualTo(EvalPassRateTrend.Direction.IMPROVING);
        assertThat(r.slopeMedian()).isGreaterThan(0);
    }

    @Test
    void degradingTrendDetected() {
        var r = EvalPassRateTrend.analyze(List.of(0.9, 0.8, 0.6, 0.4));
        assertThat(r.direction()).isEqualTo(EvalPassRateTrend.Direction.DEGRADING);
        assertThat(r.slopeMedian()).isLessThan(0);
    }

    @Test
    void outlierRunDoesNotDistortMedianSlope() {
        // 稳定 0.5 系列中混入一个 0.9 离群（均值斜率会被拉偏，中位数不受）
        var r = EvalPassRateTrend.analyze(List.of(0.5, 0.5, 0.9, 0.5, 0.5));
        assertThat(r.direction()).isEqualTo(EvalPassRateTrend.Direction.STABLE);
    }

    @Test
    void stableBandAroundZero() {
        var r = EvalPassRateTrend.analyze(List.of(0.50, 0.502, 0.498, 0.501));
        assertThat(r.direction()).isEqualTo(EvalPassRateTrend.Direction.STABLE);
    }

    @Test
    void passRatesPreservedInReport() {
        List<Double> rates = List.of(0.2, 0.4, 0.6);
        var r = EvalPassRateTrend.analyze(rates);
        assertThat(r.passRates()).containsExactlyElementsOf(rates);
        assertThat(r.runs()).isEqualTo(3);
    }
}
