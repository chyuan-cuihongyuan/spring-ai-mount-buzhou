package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * DouglasPeucker 契约测试（spec 10014 / X10030）：共线收缩 + 拐点
 * 保留手锚 + 子集与端点契约 + 抽稀不丢大偏差点 + fail-fast。
 */
class DouglasPeuckerTest {

    private static final double EPSILON = 1e-9;

    @Test
    void shouldCollapseCollinearRunToEndpoints() {
        double[][] line = {{0, 0}, {1, 1}, {2, 2}, {3, 3}, {4, 4}};
        List<double[]> result = DouglasPeucker.simplify(line, 0.5);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)[0]).isCloseTo(0.0, within(EPSILON));
        assertThat(result.get(1)[0]).isCloseTo(4.0, within(EPSILON));
    }

    @Test
    void shouldKeepBigDeviationKnee() {
        double[][] zigzag = {{0, 0}, {1, 0.05}, {2, 0}, {3, 5}, {4, 0}};
        List<double[]> result = DouglasPeucker.simplify(zigzag, 0.5);
        assertThat(result).hasSize(4);
        assertThat(result.get(2)[1]).isCloseTo(5.0, within(EPSILON));
        assertThat(result.get(2)[0]).isCloseTo(3.0, within(EPSILON));
    }

    @Test
    void shouldPreserveEndpointsAndSubsetProperty() {
        Random random = new Random(20260930L);
        for (int trial = 0; trial < 50; trial++) {
            int n = 10 + random.nextInt(40);
            double[][] pts = new double[n][2];
            for (int i = 0; i < n; i++) {
                pts[i][0] = i;
                pts[i][1] = Math.sin(i * 0.7) + (random.nextDouble() - 0.5) * 0.2;
            }
            double tolerance = 0.1 + random.nextDouble();
            List<double[]> result = DouglasPeucker.simplify(pts, tolerance);
            assertThat(result.get(0)).as("trial %d 首端保留", trial).isEqualTo(pts[0]);
            assertThat(result.get(result.size() - 1)).as("trial %d 末端保留", trial)
                    .isEqualTo(pts[n - 1]);
            for (double[] p : result) {
                boolean contained = false;
                for (double[] q : pts) {
                    if (p == q) {
                        contained = true;
                        break;
                    }
                }
                assertThat(contained).as("trial %d 输出为输入子集", trial).isTrue();
            }
        }
    }

    @Test
    void shouldKeepAllPointsAboveTolerance() {
        double[][] pts = {{0, 0}, {1, 2}, {2, 0}, {3, 0.3}, {4, 0}};
        List<double[]> result = DouglasPeucker.simplify(pts, 0.5);
        assertThat(result).hasSize(4);
        assertThat(result.get(2)[0]).isCloseTo(2.0, within(EPSILON));
        assertThat(result.get(3)[0]).isCloseTo(4.0, within(EPSILON));
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> DouglasPeucker.simplify(null, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DouglasPeucker.simplify(new double[][]{{0, 0}}, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DouglasPeucker.simplify(
                new double[][]{{0, 0}, {1, 1}}, -0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
