package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * ForwardBackward 契约测试（spec 10018 / X10038）：手锚 + 全路径
 * 穷举互证圣像 + β 一致性 + 长序列缩放稳定 + fail-fast。
 */
class ForwardBackwardTest {

    private static final double EPSILON = 1e-9;

    /** 伞世界手锚：A/B/π 经典 Rabiner 例，P(伞,无伞|λ)=0.209。 */
    @Test
    void shouldMatchHandAnchorLikelihood() {
        double[][] a = {{0.7, 0.3}, {0.4, 0.6}};
        double[][] b = {{0.9, 0.1}, {0.2, 0.8}};
        double[] pi = {0.6, 0.4};
        ForwardBackward.Result result = ForwardBackward.run(a, b, pi, new int[]{0, 1});
        double brute = bruteForce(a, b, pi, new int[]{0, 1});
        assertThat(Math.exp(result.logLikelihood())).isCloseTo(brute, within(EPSILON));
        assertThat(brute).isCloseTo(0.209, within(1e-6));
    }

    @Test
    void shouldMatchBruteForceOnRandomModels() {
        Random random = new Random(20260930L);
        for (int trial = 0; trial < 30; trial++) {
            int n = 2 + random.nextInt(2);
            int m = 2 + random.nextInt(2);
            int t = 3 + random.nextInt(5);
            double[][] a = randomRows(n, n, random);
            double[][] b = randomRows(n, m, random);
            double[] pi = randomRows(1, n, random)[0];
            int[] obs = new int[t];
            for (int i = 0; i < t; i++) {
                obs[i] = random.nextInt(m);
            }
            ForwardBackward.Result result = ForwardBackward.run(a, b, pi, obs);
            double brute = bruteForce(a, b, pi, obs);
            assertThat(Math.exp(result.logLikelihood())).as("trial %d 穷举互证", trial)
                    .isCloseTo(brute, within(1e-9));
        }
    }

    @Test
    void shouldKeepScaledValuesFiniteOnLongSequence() {
        Random random = new Random(7L);
        int n = 3;
        int m = 4;
        double[][] a = randomRows(n, n, random);
        double[][] b = randomRows(n, m, random);
        double[] pi = randomRows(1, n, random)[0];
        int[] obs = new int[500];
        for (int i = 0; i < obs.length; i++) {
            obs[i] = random.nextInt(m);
        }
        ForwardBackward.Result result = ForwardBackward.run(a, b, pi, obs);
        for (double[] row : result.alpha()) {
            for (double v : row) {
                assertThat(v).isFinite();
                assertThat(v).isBetween(0.0, 1.0 + 1e-9);
            }
        }
        assertThat(result.logLikelihood()).isFinite().isNegative();
    }

    @Test
    void shouldFailFastOnContractViolations() {
        double[][] a = {{0.7, 0.3}, {0.4, 0.6}};
        double[][] b = {{0.9, 0.1}, {0.2, 0.8}};
        double[] pi = {0.6, 0.4};
        assertThatThrownBy(() -> ForwardBackward.run(null, b, pi, new int[]{0}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ForwardBackward.run(a, b, pi, new int[]{2}))
                .isInstanceOf(IllegalArgumentException.class);
        double[][] negative = {{1.2, -0.2}, {0.4, 0.6}};
        assertThatThrownBy(() -> ForwardBackward.run(negative, b, pi, new int[]{0}))
                .isInstanceOf(IllegalArgumentException.class);
        double[][] zeroEmission = {{0.0, 0.0}, {0.0, 0.0}};
        assertThatThrownBy(() -> ForwardBackward.run(a, zeroEmission, pi, new int[]{0}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private double[][] randomRows(int rows, int cols, Random random) {
        double[][] m = new double[rows][cols];
        for (int r = 0; r < rows; r++) {
            double sum = 0.0;
            for (int c = 0; c < cols; c++) {
                m[r][c] = random.nextDouble() + 0.05;
                sum += m[r][c];
            }
            for (int c = 0; c < cols; c++) {
                m[r][c] /= sum;
            }
        }
        return m;
    }

    private double bruteForce(double[][] a, double[][] b, double[] pi, int[] obs) {
        int n = pi.length;
        int t = obs.length;
        double total = 0.0;
        int[] path = new int[t];
        for (int mask = 0; mask < (1 << t * 2); mask++) {
            int value = mask;
            boolean valid = true;
            for (int i = 0; i < t; i++) {
                path[i] = value & 3;
                if (path[i] >= n) {
                    valid = false;
                    break;
                }
                value >>= 2;
            }
            if (!valid) {
                continue;
            }
            double p = pi[path[0]] * b[path[0]][obs[0]];
            for (int i = 1; i < t && p > 0; i++) {
                p *= a[path[i - 1]][path[i]] * b[path[i]][obs[i]];
            }
            total += p;
        }
        return total;
    }
}
