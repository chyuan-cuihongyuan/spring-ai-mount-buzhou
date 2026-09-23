package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.TheilSenSlope.Fit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 4038 / T6078：Theil-Sen 合同——精确线复原、离群免疫
 * （OLS 对照）、手算下中位、退化/畸形 fail-fast。
 */
class TheilSenSlopeTest {

    @Test
    void exactLineShouldBeRecoveredExactly() {
        double[] xs = {0, 1, 2, 3, 4, 5};
        double[] ys = {2, 5, 8, 11, 14, 17};   // y = 3x + 2
        Fit fit = TheilSenSlope.fit(xs, ys);
        assertThat(fit.slope()).isCloseTo(3.0, within(1e-9));
        assertThat(fit.intercept()).isCloseTo(2.0, within(1e-9));
        assertThat(fit.predict(10)).isCloseTo(32.0, within(1e-9));
    }

    @Test
    void outlierShouldMoveOlsFarButStayTheilSenStable() {
        double[] xs = {0, 1, 2, 3, 4, 5, 6};
        double[] ys = {0, 1, 2, 3, 4, 5, 1006};   // 末点 +1000 污染
        Fit robust = TheilSenSlope.fit(xs, ys);
        assertThat(robust.slope()).isCloseTo(1.0, within(0.5));   // TS 免疫
        double olsSlope = ordinaryLeastSquaresSlope(xs, ys);
        assertThat(Math.abs(olsSlope - 1.0)).isGreaterThan(2.0);   // OLS 被拉偏对照
    }

    @Test
    void handComputedEvenPairwiseShouldTakeLowerMedian() {
        double[] xs = {0, 1, 2, 3};
        double[] ys = {0, 10, 0, 30};
        // 成对斜率 [-10,0,10,10,10,30]——下中位（6 值取第 3 个）= 10
        // 残差 [0,0,-20,0]——下中位（4 值取第 2 个）= 0
        Fit fit = TheilSenSlope.fit(xs, ys);
        assertThat(fit.slope()).isEqualTo(10.0);
        assertThat(fit.intercept()).isEqualTo(0.0);
    }

    @Test
    void duplicatedXVerticalPairsShouldBeSkipped() {
        double[] xs = {0, 0, 1, 2};
        double[] ys = {5, 100, 10, 20};   // (0,5)/(0,100) 竖直对跳过
        Fit fit = TheilSenSlope.fit(xs, ys);
        assertThat(Double.isFinite(fit.slope())).isTrue();
        assertThat(fit.slope()).isCloseTo(10.0, within(6.0));   // 健康对主导
    }

    @Test
    void degenerateAndMalformedInputsShouldFailFast() {
        assertThatThrownBy(() -> TheilSenSlope.fit(new double[]{1, 2}, new double[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TheilSenSlope.fit(new double[]{1}, new double[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TheilSenSlope.fit(new double[]{7, 7, 7}, new double[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TheilSenSlope.fit(new double[]{1, Double.NaN}, new double[]{1, 2}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** OLS 斜率对照（测试内最小实现）。 */
    private static double ordinaryLeastSquaresSlope(double[] xs, double[] ys) {
        double meanX = 0;
        double meanY = 0;
        for (int i = 0; i < xs.length; i++) {
            meanX += xs[i];
            meanY += ys[i];
        }
        meanX /= xs.length;
        meanY /= ys.length;
        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < xs.length; i++) {
            numerator += (xs[i] - meanX) * (ys[i] - meanY);
            denominator += (xs[i] - meanX) * (xs[i] - meanX);
        }
        return numerator / denominator;
    }
}
