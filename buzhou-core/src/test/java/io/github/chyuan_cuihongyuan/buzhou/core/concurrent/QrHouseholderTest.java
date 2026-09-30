package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * QrHouseholder 契约测试（spec 10009 / X10020）：Q 正交圣像 + QR
 * 重构 + R 上三角 + 最小二乘对法方程互证 + fail-fast。
 */
class QrHouseholderTest {

    private static final double EPSILON = 1e-8;

    private double[][] randomTall(int m, int n, Random random) {
        double[][] a = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = random.nextDouble() * 4.0 - 2.0;
            }
        }
        return a;
    }

    @Test
    void shouldBeOrthogonalAndReconstructA() {
        Random random = new Random(20260930L);
        for (int trial = 0; trial < 30; trial++) {
            int m = 4 + random.nextInt(6);
            int n = 2 + random.nextInt(m - 3);
            double[][] a = randomTall(m, n, random);
            QrHouseholder qr = new QrHouseholder(a);
            double[][] q = qr.qFactor();
            double[][] r = qr.rFactor();
            for (int j1 = 0; j1 < n; j1++) {
                for (int j2 = 0; j2 < n; j2++) {
                    double dot = 0.0;
                    for (int i = 0; i < m; i++) {
                        dot += q[i][j1] * q[i][j2];
                    }
                    assertThat(dot).as("trial %d 正交 [%d][%d]", trial, j1, j2)
                            .isCloseTo(j1 == j2 ? 1.0 : 0.0, within(EPSILON));
                }
            }
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    double sum = 0.0;
                    for (int k = 0; k < n; k++) {
                        sum += q[i][k] * r[k][j];
                    }
                    assertThat(sum).as("trial %d QR[%d][%d]", trial, i, j)
                            .isCloseTo(a[i][j], within(EPSILON));
                }
            }
            for (int i = 1; i < n; i++) {
                for (int j = 0; j < n && j < i; j++) {
                    assertThat(r[i][j]).as("trial %d R 下三角清零[%d][%d]", trial, i, j)
                            .isCloseTo(0.0, within(EPSILON));
                }
            }
        }
    }

    @Test
    void shouldSolveLeastSquaresMatchingNormalEquations() {
        Random random = new Random(42L);
        for (int trial = 0; trial < 30; trial++) {
            int m = 6 + random.nextInt(5);
            int n = 2 + random.nextInt(3);
            double[][] a = randomTall(m, n, random);
            double[] b = new double[m];
            for (int i = 0; i < m; i++) {
                b[i] = random.nextDouble() * 10.0 - 5.0;
            }
            double[] viaQr = new QrHouseholder(a).solve(b);
            double[][] ata = new double[n][n];
            double[] atb = new double[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    for (int k = 0; k < m; k++) {
                        ata[i][j] += a[k][i] * a[k][j];
                    }
                }
                for (int k = 0; k < m; k++) {
                    atb[i] += a[k][i] * b[k];
                }
            }
            double[] viaNormal = GaussianElimination.solve(ata, atb);
            for (int i = 0; i < n; i++) {
                assertThat(viaQr[i]).as("trial %d 最小二乘互证分量 %d", trial, i)
                        .isCloseTo(viaNormal[i], within(1e-6));
            }
        }
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> new QrHouseholder(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QrHouseholder(new double[][]{{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QrHouseholder(new double[][]{{1.0, 2.0}, {3.0}}))
                .isInstanceOf(IllegalArgumentException.class);
        QrHouseholder qr = new QrHouseholder(
                new double[][]{{1.0, 0.0}, {0.0, 1.0}, {0.0, 0.0}});
        assertThatThrownBy(() -> qr.solve(new double[]{1.0}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
