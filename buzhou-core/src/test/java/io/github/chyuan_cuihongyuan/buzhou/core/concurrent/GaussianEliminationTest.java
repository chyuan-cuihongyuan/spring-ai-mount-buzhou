package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * GaussianElimination 契约测试（spec 10006 / X10014）：手锚 + 随机
 * 可解系统残差上界 + 行列式对拍 + 奇异 fail-fast + 置换鲁棒性。
 */
class GaussianEliminationTest {

    private static final double EPSILON = 1e-8;

    @Test
    void shouldSolveHandAnchor() {
        double[] x = GaussianElimination.solve(
                new double[][]{{2.0, 1.0}, {1.0, 3.0}}, new double[]{5.0, 10.0});
        assertThat(x[0]).isCloseTo(1.0, within(EPSILON));
        assertThat(x[1]).isCloseTo(3.0, within(EPSILON));
    }

    @Test
    void shouldSolveRandomSystemsWithTinyResidual() {
        Random random = new Random(20260930L);
        for (int trial = 0; trial < 100; trial++) {
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
            double[] x = GaussianElimination.solve(a, b);
            for (int i = 0; i < n; i++) {
                assertThat(x[i]).as("trial %d 分量 %d", trial, i)
                        .isCloseTo(xTrue[i], within(EPSILON));
            }
        }
    }

    @Test
    void shouldHandleZeroPivotNeedingRowSwap() {
        double[] x = GaussianElimination.solve(
                new double[][]{{0.0, 2.0}, {4.0, 1.0}}, new double[]{4.0, 9.0});
        assertThat(x[0]).isCloseTo(1.75, within(EPSILON));
        assertThat(x[1]).isCloseTo(2.0, within(EPSILON));
    }

    @Test
    void shouldComputeDeterminantMatchingHandValue() {
        double det = GaussianElimination.determinant(
                new double[][]{{3.0, 8.0}, {4.0, 6.0}});
        assertThat(det).isCloseTo(-14.0, within(EPSILON));
    }

    @Test
    void shouldReturnZeroDeterminantForSingular() {
        double det = GaussianElimination.determinant(
                new double[][]{{1.0, 2.0}, {2.0, 4.0}});
        assertThat(det).isCloseTo(0.0, within(EPSILON));
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> GaussianElimination.solve(null, new double[]{1.0}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GaussianElimination.solve(
                new double[][]{{1.0, 2.0}}, new double[]{1.0}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GaussianElimination.solve(
                new double[][]{{1.0, 2.0}, {3.0, 4.0}}, new double[]{1.0}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GaussianElimination.solve(
                new double[][]{{1.0, 2.0}, {2.0, 4.0}}, new double[]{1.0, 2.0}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GaussianElimination.determinant(
                new double[][]{{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
