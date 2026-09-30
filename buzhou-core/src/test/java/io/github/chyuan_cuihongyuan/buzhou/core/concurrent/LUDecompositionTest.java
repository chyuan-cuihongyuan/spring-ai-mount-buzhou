package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * LUDecomposition 契约测试（spec 10007 / X10016）：PA=LU 重构圣像
 * + 与 GaussianElimination 跨件互证 + 行列式对拍 + fail-fast。
 */
class LUDecompositionTest {

    private static final double EPSILON = 1e-8;

    @Test
    void shouldReconstructPAEqualsLU() {
        Random random = new Random(20260930L);
        for (int trial = 0; trial < 50; trial++) {
            int n = 2 + random.nextInt(8);
            double[][] a = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    a[i][j] = random.nextDouble() * 4.0 - 2.0;
                }
                a[i][i] += 4.0;
            }
            LUDecomposition lu = new LUDecomposition(a);
            double[][] l = lu.lowerFactor();
            double[][] u = lu.upperFactor();
            int[] perm = lu.rowPermutation();
            double[][] pa = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double sum = 0.0;
                    for (int k = 0; k < n; k++) {
                        sum += l[i][k] * u[k][j];
                    }
                    pa[i][j] = sum;
                }
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    assertThat(pa[i][j]).as("trial %d PA[%d][%d]", trial, i, j)
                            .isCloseTo(a[perm[i]][j], within(EPSILON));
                }
            }
        }
    }

    @Test
    void shouldMatchGaussianEliminationOnRandomSystems() {
        Random random = new Random(42L);
        for (int trial = 0; trial < 60; trial++) {
            int n = 2 + random.nextInt(8);
            double[][] a = new double[n][n];
            double[] xTrue = new double[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    a[i][j] = random.nextDouble() * 4.0 - 2.0;
                }
                a[i][i] += 4.0;
                xTrue[i] = random.nextDouble() * 10.0 - 5.0;
            }
            double[] b = new double[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    b[i] += a[i][j] * xTrue[j];
                }
            }
            double[] viaLu = new LUDecomposition(a).solve(b);
            double[] viaGauss = GaussianElimination.solve(a, b);
            for (int i = 0; i < n; i++) {
                assertThat(viaLu[i]).as("trial %d 跨件互证分量 %d", trial, i)
                        .isCloseTo(viaGauss[i], within(EPSILON));
            }
        }
    }

    @Test
    void shouldMatchDeterminantOfGaussianElimination() {
        Random random = new Random(7L);
        for (int trial = 0; trial < 40; trial++) {
            int n = 2 + random.nextInt(6);
            double[][] a = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    a[i][j] = random.nextDouble() * 4.0 - 2.0;
                }
                a[i][i] += 4.0;
            }
            assertThat(new LUDecomposition(a).determinant())
                    .isCloseTo(GaussianElimination.determinant(a), within(1e-8));
        }
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> new LUDecomposition(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LUDecomposition(new double[][]{{1.0, 2.0}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LUDecomposition(
                new double[][]{{1.0, 2.0}, {2.0, 4.0}}))
                .isInstanceOf(IllegalArgumentException.class);
        LUDecomposition lu = new LUDecomposition(
                new double[][]{{2.0, 0.0}, {0.0, 3.0}});
        assertThatThrownBy(() -> lu.solve(new double[]{1.0}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
