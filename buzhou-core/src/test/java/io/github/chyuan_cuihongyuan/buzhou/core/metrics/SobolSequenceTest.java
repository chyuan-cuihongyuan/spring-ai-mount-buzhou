package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * SobolSequence 契约测试（spec 10024 / X10050）：值域 + 一维二分
 * 分层圣像 + 首点零 + 二维分层 + 确定性 + fail-fast。
 */
class SobolSequenceTest {

    @Test
    void shouldBeInUnitHypercube() {
        double[][] points = SobolSequence.sample(3, 256);
        for (double[] point : points) {
            for (double v : point) {
                assertThat(v).isBetween(0.0, 1.0);
            }
        }
    }

    @Test
    void shouldStartAtOrigin() {
        for (int d = 1; d <= 4; d++) {
            assertThat(SobolSequence.coordinate(d, 0L)).isCloseTo(0.0, within(1e-12));
        }
    }

    @Test
    void shouldStratifyDyadicIntervalsDimensionOne() {
        int samples = 32;
        int intervals = 8;
        int[] counts = new int[intervals];
        for (int i = 0; i < samples; i++) {
            double v = SobolSequence.coordinate(1, i);
            counts[(int) (v * intervals)]++;
        }
        for (int j = 0; j < intervals; j++) {
            assertThat(counts[j]).as("一维区间 %d 分层均匀", j).isEqualTo(samples / intervals);
        }
    }

    @Test
    void shouldStratifyTwoDimensionalQuadrants() {
        int samples = 16;
        int grid = 4;
        int[][] counts = new int[grid][grid];
        for (int i = 0; i < samples; i++) {
            double x = SobolSequence.coordinate(1, i);
            double y = SobolSequence.coordinate(2, i);
            counts[(int) (x * grid)][(int) (y * grid)]++;
        }
        for (int gx = 0; gx < grid; gx++) {
            for (int gy = 0; gy < grid; gy++) {
                assertThat(counts[gx][gy]).as("二维格 [%d][%d]", gx, gy).isEqualTo(1);
            }
        }
    }

    @Test
    void shouldBeDeterministicAcrossRuns() {
        double[][] first = SobolSequence.sample(2, 64);
        double[][] second = SobolSequence.sample(2, 64);
        assertThat(java.util.Arrays.deepEquals(first, second)).isTrue();
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> SobolSequence.coordinate(0, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SobolSequence.coordinate(9, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SobolSequence.coordinate(1, -1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SobolSequence.sample(2, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
