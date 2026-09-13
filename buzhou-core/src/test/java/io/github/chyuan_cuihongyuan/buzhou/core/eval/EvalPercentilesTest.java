package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * impl-662 / spec 909：R-7 分位数——已知值精确（P50 偶数样本=中间两数均值）、
 * 单调 q 单调值、单样本退化、参数校验、输出保序。
 */
class EvalPercentilesTest {

    private static final double[] SAMPLES = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};

    @Test
    void knownR7Values() {
        // n=10：P50 → h=4.5 → sorted[4]=0.5 与 sorted[5]=0.6 插值=0.55（偶数样本=中间均值）；P25 → h=2.25 → 0.3+0.25×0.1=0.325
        Map<Double, Double> out = EvalScoreAnalytics.percentiles(SAMPLES, 0.5, 0.25);
        assertThat(out.get(0.5)).isCloseTo(0.55, within(1e-12));
        assertThat(out.get(0.25)).isCloseTo(0.325, within(1e-12));
    }

    @Test
    void monotonicQuantilesMonotonicValues() {
        Map<Double, Double> out = EvalScoreAnalytics.percentiles(SAMPLES,
                0.1, 0.3, 0.5, 0.7, 0.9);
        double prev = Double.NEGATIVE_INFINITY;
        for (double v : out.values()) {
            assertThat(v).isGreaterThanOrEqualTo(prev);
            prev = v;
        }
    }

    @Test
    void singleSampleDegeneratesToConstant() {
        Map<Double, Double> out = EvalScoreAnalytics.percentiles(new double[]{0.42}, 0.1, 0.9);
        assertThat(out.get(0.1)).isEqualTo(0.42);
        assertThat(out.get(0.9)).isEqualTo(0.42);
    }

    @Test
    void preservesQuantileOrder() {
        Map<Double, Double> out = EvalScoreAnalytics.percentiles(SAMPLES, 0.9, 0.1);
        assertThat(out.keySet()).containsExactly(0.9, 0.1); // 入参序非排序序
    }

    @Test
    void argsValidated() {
        assertThatThrownBy(() -> EvalScoreAnalytics.percentiles(new double[0], 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvalScoreAnalytics.percentiles(new double[]{Double.NaN}, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvalScoreAnalytics.percentiles(SAMPLES))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvalScoreAnalytics.percentiles(SAMPLES, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvalScoreAnalytics.percentiles(SAMPLES, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
