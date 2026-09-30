package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * BaumWelch 契约测试（spec 10019 / X10040）：EM 似然单调不减圣像
 * + 单步重估与穷举后验互证 + 行随机归一 + fail-fast。
 */
class BaumWelchTest {

    private static final double EPSILON = 1e-7;

    @Test
    void shouldMonotonicallyIncreaseLikelihood() {
        Random random = new Random(20260930L);
        for (int trial = 0; trial < 10; trial++) {
            double[][] a = randomRows(2, 2, random);
            double[][] b = randomRows(2, 3, random);
            double[] pi = randomRows(1, 2, random)[0];
            int[] obs = new int[8];
            for (int i = 0; i < obs.length; i++) {
                obs[i] = random.nextInt(3);
            }
            double previous = ForwardBackward.run(a, b, pi, obs).logLikelihood();
            for (int iter = 1; iter <= 5; iter++) {
                double current = BaumWelch.reestimate(a, b, pi, obs, iter).logLikelihood();
                assertThat(current).as("trial %d 第 %d 轮似然单调不减", trial, iter)
                        .isGreaterThanOrEqualTo(previous - 1e-9);
                previous = current;
            }
        }
    }

    @Test
    void shouldMatchBruteForcePosteriorOneStep() {
        Random random = new Random(7L);
        double[][] a = randomRows(2, 2, random);
        double[][] b = randomRows(2, 2, random);
        double[] pi = randomRows(1, 2, random)[0];
        int[] obs = {0, 1, 0, 1};
        BaumWelch.Estimate estimate = BaumWelch.reestimate(a, b, pi, obs, 1);
        double[][] expected = bruteForceReestimate(a, b, pi, obs);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                assertThat(estimate.a()[i][j]).as("穷举后验互证 A[%d][%d]", i, j)
                        .isCloseTo(expected[i][j], within(1e-6));
            }
        }
    }

    @Test
    void shouldKeepRowsStochastic() {
        Random random = new Random(11L);
        double[][] a = randomRows(3, 3, random);
        double[][] b = randomRows(3, 4, random);
        double[] pi = randomRows(1, 3, random)[0];
        int[] obs = new int[12];
        for (int i = 0; i < obs.length; i++) {
            obs[i] = random.nextInt(4);
        }
        BaumWelch.Estimate estimate = BaumWelch.reestimate(a, b, pi, obs, 3);
        for (double[] row : estimate.a()) {
            assertThat(sumOf(row)).isCloseTo(1.0, within(1e-9));
        }
        for (double[] row : estimate.b()) {
            assertThat(sumOf(row)).isCloseTo(1.0, within(1e-9));
        }
        assertThat(sumOf(estimate.pi())).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void shouldFailFastOnContractViolations() {
        double[][] a = {{0.7, 0.3}, {0.4, 0.6}};
        double[][] b = {{0.9, 0.1}, {0.2, 0.8}};
        double[] pi = {0.6, 0.4};
        assertThatThrownBy(() -> BaumWelch.reestimate(a, b, pi, new int[]{0}, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BaumWelch.reestimate(a, b, pi, new int[]{5}, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private double sumOf(double[] v) {
        double s = 0.0;
        for (double x : v) {
            s += x;
        }
        return s;
    }

    private double[][] randomRows(int rows, int cols, Random random) {
        double[][] m = new double[rows][cols];
        for (int r = 0; r < rows; r++) {
            double sum = 0.0;
            for (int c = 0; c < cols; c++) {
                m[r][c] = random.nextDouble() + 0.1;
                sum += m[r][c];
            }
            for (int c = 0; c < cols; c++) {
                m[r][c] /= sum;
            }
        }
        return m;
    }

    /** 全路径穷举后验 A 重估（2 状态 T=4 教学锚）。 */
    private double[][] bruteForceReestimate(double[][] a, double[][] b, double[] pi, int[] obs) {
        double[][] counts = new double[2][2];
        double evidence = 0.0;
        for (int s0 = 0; s0 < 2; s0++) {
            for (int s1 = 0; s1 < 2; s1++) {
                for (int s2 = 0; s2 < 2; s2++) {
                    for (int s3 = 0; s3 < 2; s3++) {
                        int[] path = {s0, s1, s2, s3};
                        double p = pi[s0] * b[s0][obs[0]];
                        for (int i = 1; i < 4; i++) {
                            p *= a[path[i - 1]][path[i]] * b[path[i]][obs[i]];
                        }
                        evidence += p;
                        counts[s0][s1] += p;
                        counts[s1][s2] += p;
                        counts[s2][s3] += p;
                    }
                }
            }
        }
        double[][] result = new double[2][2];
        for (int i = 0; i < 2; i++) {
            double rowSum = counts[i][0] + counts[i][1];
            for (int j = 0; j < 2; j++) {
                result[i][j] = counts[i][j] / rowSum;
            }
        }
        return result;
    }
}
