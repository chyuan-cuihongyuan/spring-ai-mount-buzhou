package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * ConjugateGradient 契约测试（spec 10010 / X10022）：随机 SPD 收敛
 * 圣像 + 与 GaussianElimination 跨件互证 + K 步理论收敛读数 +
 * 非正定 fail-fast。
 */
class ConjugateGradientTest {

    private static final double TOLERANCE = 1e-10;
    private static final double EPSILON = 1e-6;

    private double[][] randomSpd(int n, Random random) {
        double[][] a = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                double v = random.nextDouble() * 2.0 - 1.0;
                a[i][j] = v;
                a[j][i] = v;
            }
            a[i][i] += n;
        }
        return a;
    }

    @Test
    void shouldSolveRandomSpdSystemsWithTinyResidual() {
        Random random = new Random(20260930L);
        for (int trial = 0; trial < 40; trial++) {
            int n = 2 + random.nextInt(20);
            double[][] a = randomSpd(n, random);
            double[] xTrue = new double[n];
            for (int i = 0; i < n; i++) {
                xTrue[i] = random.nextDouble() * 10.0 - 5.0;
            }
            double[] b = new double[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    b[i] += a[i][j] * xTrue[j];
                }
            }
            ConjugateGradient.Result result = ConjugateGradient.solve(
                    ConjugateGradient.denseOperator(a), b, TOLERANCE, 10 * n);
            for (int i = 0; i < n; i++) {
                assertThat(result.x()[i]).as("trial %d 分量 %d", trial, i)
                        .isCloseTo(xTrue[i], within(EPSILON));
            }
        }
    }

    @Test
    void shouldConvergeWithinNDimensionsSteps() {
        Random random = new Random(7L);
        int n = 12;
        double[][] a = randomSpd(n, random);
        double[] b = new double[n];
        for (int i = 0; i < n; i++) {
            b[i] = random.nextDouble() + 0.5;
        }
        ConjugateGradient.Result result = ConjugateGradient.solve(
                ConjugateGradient.denseOperator(a), b, TOLERANCE, 100);
        assertThat(result.iterations()).as("良态 SPD 理论 K 步收敛").isLessThanOrEqualTo(n);
    }

    @Test
    void shouldMatchGaussianElimination() {
        Random random = new Random(42L);
        int n = 8;
        double[][] a = randomSpd(n, random);
        double[] b = new double[n];
        for (int i = 0; i < n; i++) {
            b[i] = random.nextDouble() * 6.0 - 3.0;
        }
        double[] viaCg = ConjugateGradient.solve(
                ConjugateGradient.denseOperator(a), b, TOLERANCE, 200).x();
        double[] viaGauss = GaussianElimination.solve(a, b);
        for (int i = 0; i < n; i++) {
            assertThat(viaCg[i]).isCloseTo(viaGauss[i], within(EPSILON));
        }
    }

    @Test
    void shouldFailFastOnContractViolations() {
        double[][] a = {{2.0, 0.0}, {0.0, 3.0}};
        assertThatThrownBy(() -> ConjugateGradient.solve(
                ConjugateGradient.denseOperator(a), null, TOLERANCE, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConjugateGradient.solve(
                null, new double[]{1.0}, TOLERANCE, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConjugateGradient.solve(
                ConjugateGradient.denseOperator(a), new double[]{1.0, 2.0}, 0.0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConjugateGradient.solve(
                ConjugateGradient.denseOperator(a), new double[]{1.0, 2.0}, TOLERANCE, 0))
                .isInstanceOf(IllegalArgumentException.class);
        double[][] indefinite = {{1.0, 0.0}, {0.0, -1.0}};
        assertThatThrownBy(() -> ConjugateGradient.solve(
                ConjugateGradient.denseOperator(indefinite), new double[]{1.0, 1.0}, TOLERANCE, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
