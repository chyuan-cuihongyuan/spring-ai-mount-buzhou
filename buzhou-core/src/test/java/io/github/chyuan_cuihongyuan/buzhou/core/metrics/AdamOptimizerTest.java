package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 10045 / X10091：AdamOptimizer 合同验证——二次碗解析解收敛
 * +首步手算锚+梯度零不动+确定性+fail-fast。
 */
class AdamOptimizerTest {

    /** f(w)=½Σaᵢwᵢ²−Σbᵢwᵢ，解析极小 b/a。 */
    private static AdamOptimizer.Objective diagonalBowl(double[] a, double[] b) {
        return new AdamOptimizer.Objective() {
            @Override
            public double valueAt(double[] w) {
                double value = 0;
                for (int i = 0; i < w.length; i++) {
                    value += 0.5 * a[i] * w[i] * w[i] - b[i] * w[i];
                }
                return value;
            }

            @Override
            public double[] gradientAt(double[] w) {
                double[] g = new double[w.length];
                for (int i = 0; i < w.length; i++) {
                    g[i] = a[i] * w[i] - b[i];
                }
                return g;
            }
        };
    }

    @Test
    void shouldConvergeToAnalyticSolution_whenDiagonalBowl() {
        double[] a = {1.0, 10.0};
        double[] b = {2.0, -3.0};
        double[] solution = AdamOptimizer.minimize(diagonalBowl(a, b),
                new double[]{0, 0}, 0.05, 0.9, 0.999, 1e-8, 20000);
        assertThat(solution[0]).isCloseTo(2.0, within(1e-3));
        assertThat(solution[1]).isCloseTo(-0.3, within(1e-3));
    }

    @Test
    void shouldMatchManualFirstStep_whenSingleStep() {
        // f(w)=½·4w² → g(w0)=4w0；w0=0.5 → g=2。首步 m̂=2、v̂=4 →
        // 步长=lr·2/(√4+ε)≈lr → w1≈0.5−lr
        double[] result = AdamOptimizer.minimize(diagonalBowl(new double[]{4}, new double[]{0}),
                new double[]{0.5}, 0.1, 0.9, 0.999, 1e-8, 1);
        assertThat(result[0]).isCloseTo(0.4, within(1e-6));
    }

    @Test
    void shouldStayPut_whenGradientAlreadyZero() {
        double[] result = AdamOptimizer.minimize(diagonalBowl(new double[]{4}, new double[]{0}),
                new double[]{0}, 0.1, 0.9, 0.999, 1e-8, 100);
        assertThat(result[0]).isCloseTo(0.0, within(1e-12));
    }

    @Test
    void shouldReproduceIdenticalPath_whenSameInputTwice() {
        double[] a = {1.0, 5.0};
        double[] b = {1.0, 2.0};
        assertThat(AdamOptimizer.minimize(diagonalBowl(a, b),
                new double[]{3, -1}, 0.05, 0.9, 0.999, 1e-8, 500))
                .isEqualTo(AdamOptimizer.minimize(diagonalBowl(a, b),
                        new double[]{3, -1}, 0.05, 0.9, 0.999, 1e-8, 500));
    }

    @Test
    void shouldFailFast_whenInvalidHyperparameters() {
        AdamOptimizer.Objective bowl = diagonalBowl(new double[]{1}, new double[]{0});
        assertThatThrownBy(() -> AdamOptimizer.minimize(bowl, new double[]{0}, 0, 0.9, 0.999, 1e-8, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("步长");
        assertThatThrownBy(() -> AdamOptimizer.minimize(bowl, new double[]{0}, 0.1, 1.0, 0.999, 1e-8, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("β");
        assertThatThrownBy(() -> AdamOptimizer.minimize(bowl, new double[]{0}, 0.1, 0.9, 0.999, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ε");
        assertThatThrownBy(() -> AdamOptimizer.minimize(bowl, new double[]{0}, 0.1, 0.9, 0.999, 1e-8, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("迭代数");
        assertThatThrownBy(() -> AdamOptimizer.minimize(null, new double[]{0}, 0.1, 0.9, 0.999, 1e-8, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
