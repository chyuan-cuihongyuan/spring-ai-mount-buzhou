package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * CholeskyDecomposition 契约测试（spec 10008 / X10018）：LLᵀ 重构
 * 圣像 + 与 GaussianElimination 跨件互证 + 行列式恒等 + fail-fast。
 */
class CholeskyDecompositionTest {

    private static final double EPSILON = 1e-8;

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
    void shouldReconstructLowerTimesLowerTranspose() {
        Random random = new Random(20260930L);
        for (int trial = 0; trial < 40; trial++) {
            int n = 2 + random.nextInt(7);
            double[][] a = randomSpd(n, random);
            double[][] l = new CholeskyDecomposition(a).lowerFactor();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double sum = 0.0;
                    for (int k = 0; k < n; k++) {
                        sum += l[i][k] * l[j][k];
                    }
                    assertThat(sum).as("trial %d LLᵀ[%d][%d]", trial, i, j)
                            .isCloseTo(a[i][j], within(1e-8));
                }
            }
        }
    }

    @Test
    void shouldMatchGaussianEliminationOnRandomSystems() {
        Random random = new Random(42L);
        for (int trial = 0; trial < 50; trial++) {
            int n = 2 + random.nextInt(7);
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
            double[] viaChol = new CholeskyDecomposition(a).solve(b);
            double[] viaGauss = GaussianElimination.solve(a, b);
            for (int i = 0; i < n; i++) {
                assertThat(viaChol[i]).as("trial %d 跨件互证分量 %d", trial, i)
                        .isCloseTo(viaGauss[i], within(EPSILON));
            }
        }
    }

    @Test
    void shouldMatchDeterminantIdentity() {
        Random random = new Random(7L);
        for (int trial = 0; trial < 30; trial++) {
            int n = 2 + random.nextInt(6);
            double[][] a = randomSpd(n, random);
            assertThat(new CholeskyDecomposition(a).determinant())
                    .isCloseTo(GaussianElimination.determinant(a), within(1e-6));
        }
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> new CholeskyDecomposition(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CholeskyDecomposition(new double[][]{{1.0, 2.0}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CholeskyDecomposition(
                new double[][]{{1.0, 2.0}, {3.0, 4.0}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CholeskyDecomposition(
                new double[][]{{1.0, 0.0}, {0.0, -4.0}}))
                .isInstanceOf(IllegalArgumentException.class);
        CholeskyDecomposition chol = new CholeskyDecomposition(
                new double[][]{{4.0, 0.0}, {0.0, 9.0}});
        assertThatThrownBy(() -> chol.solve(new double[]{1.0}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
