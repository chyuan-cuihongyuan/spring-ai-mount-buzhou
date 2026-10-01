package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 10044 / X10089：LinearRegression 合同验证——精确系数复原+多元面
 * +残差正交性+奇异 fail-fast+确定性。
 */
class LinearRegressionTest {

    @Test
    void shouldRecoverExactCoefficients_whenNoiseFreeLine() {
        double[][] x = {{1}, {2}, {3}, {4}};
        double[] y = {3, 5, 7, 9};   // y = 2x + 1
        double[] weights = LinearRegression.fit(x, y);
        assertThat(weights[0]).isCloseTo(1.0, within(1e-9));
        assertThat(weights[1]).isCloseTo(2.0, within(1e-9));
    }

    @Test
    void shouldFitTwoFeatures_whenNoCollinearity() {
        double[][] x = {{1, 0}, {2, 1}, {3, 0}, {4, 1}};
        double[] y = {3, 6, 7, 10};  // y = 1 + 2a + 1b
        double[] weights = LinearRegression.fit(x, y);
        assertThat(weights[0]).isCloseTo(1.0, within(1e-9));
        assertThat(weights[1]).isCloseTo(2.0, within(1e-9));
        assertThat(weights[2]).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void shouldHaveOrthogonalResiduals_whenNoisyLeastSquares() {
        double[][] x = {{0}, {1}, {2}, {3}, {4}};
        double[] y = {0.1, 2.0, 4.2, 5.9, 8.3};
        double[] weights = LinearRegression.fit(x, y);
        // 残差正交性：Aᵀ(y−Xw) ≈ 0（截距列与特征列两向）
        double interceptDot = 0;
        double featureDot = 0;
        for (int i = 0; i < x.length; i++) {
            double predicted = weights[0] + weights[1] * x[i][0];
            interceptDot += y[i] - predicted;
            featureDot += x[i][0] * (y[i] - predicted);
        }
        assertThat(Math.abs(interceptDot)).isLessThan(1e-9);
        assertThat(Math.abs(featureDot)).isLessThan(1e-9);
    }

    @Test
    void shouldReproduceIdenticalWeights_whenSameInputTwice() {
        double[][] x = {{1}, {2}, {3}};
        double[] y = {2, 4, 6};
        assertThat(LinearRegression.fit(x, y)).isEqualTo(LinearRegression.fit(x, y));
    }

    @Test
    void shouldFailFast_whenDuplicateFeatureCausesSingularity() {
        double[][] x = {{1, 1}, {2, 2}, {3, 3}};
        double[] y = {1, 2, 3};
        assertThatThrownBy(() -> LinearRegression.fit(x, y))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LinearRegression.fit(new double[][]{{1}}, new double[2]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("长度不配");
    }
}
