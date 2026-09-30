package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * HaltonSequence 契约测试（spec 10025 / X10052）：手锚 + 值域 +
 * 基 2 分层 + 首点零 + 确定性 + fail-fast。
 */
class HaltonSequenceTest {

    private static final double EPSILON = 1e-9;

    @Test
    void shouldMatchHandAnchors() {
        assertThat(HaltonSequence.radicalInverse(1L, 2)).isCloseTo(0.5, within(EPSILON));
        assertThat(HaltonSequence.radicalInverse(2L, 2)).isCloseTo(0.25, within(EPSILON));
        assertThat(HaltonSequence.radicalInverse(3L, 2)).isCloseTo(0.75, within(EPSILON));
        assertThat(HaltonSequence.radicalInverse(1L, 3)).isCloseTo(1.0 / 3.0, within(EPSILON));
        assertThat(HaltonSequence.radicalInverse(3L, 3)).isCloseTo(1.0 / 9.0, within(EPSILON));
        assertThat(HaltonSequence.coordinate(1, 1L)).isCloseTo(0.5, within(EPSILON));
    }

    @Test
    void shouldBeInUnitHypercube() {
        double[][] points = HaltonSequence.sample(3, 200);
        for (double[] point : points) {
            for (double v : point) {
                assertThat(v).isBetween(0.0, 1.0);
            }
        }
    }

    @Test
    void shouldStratifyBaseTwoDimensionOne() {
        int samples = 32;
        int intervals = 8;
        int[] counts = new int[intervals];
        for (int i = 0; i < samples; i++) {
            double v = HaltonSequence.coordinate(1, i);
            counts[(int) (v * intervals)]++;
        }
        for (int j = 0; j < intervals; j++) {
            assertThat(counts[j]).as("基 2 一维区间 %d 分层", j).isEqualTo(samples / intervals);
        }
    }

    @Test
    void shouldStartAtOriginAndBeDeterministic() {
        for (int d = 1; d <= 3; d++) {
            assertThat(HaltonSequence.coordinate(d, 0L)).isCloseTo(0.0, within(1e-12));
        }
        double[][] first = HaltonSequence.sample(2, 64);
        double[][] second = HaltonSequence.sample(2, 64);
        assertThat(java.util.Arrays.deepEquals(first, second)).isTrue();
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> HaltonSequence.coordinate(0, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HaltonSequence.coordinate(9, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HaltonSequence.coordinate(1, -1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HaltonSequence.sample(2, -3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
