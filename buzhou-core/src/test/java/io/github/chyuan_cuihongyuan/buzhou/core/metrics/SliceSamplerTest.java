package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.function.DoubleUnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * SliceSampler 契约测试（spec 10028 / X10058）：标准正态矩圣像 +
 * 均匀域均值 + 确定性 + fail-fast。
 */
class SliceSamplerTest {

    private static final double EPSILON = 1e-9;

    @Test
    void shouldRecoverStandardNormalMoments() {
        DoubleUnaryOperator normalLogDensity = x -> -0.5 * x * x;
        double[] samples = SliceSampler.sample(normalLogDensity, 0.0, 20_000, 1.0, 42L);
        double mean = 0.0;
        for (double v : samples) {
            mean += v;
        }
        mean /= samples.length;
        double variance = 0.0;
        for (double v : samples) {
            variance += (v - mean) * (v - mean);
        }
        variance /= samples.length - 1;
        assertThat(mean).isCloseTo(0.0, within(0.1));
        assertThat(variance).isCloseTo(1.0, within(0.2));
    }

    @Test
    void shouldSampleUniformOnBoundedSupport() {
        DoubleUnaryOperator flatLogDensity = x -> x >= 0 && x <= 1 ? 0.0 : Double.NEGATIVE_INFINITY;
        double[] samples = SliceSampler.sample(flatLogDensity, 0.5, 10_000, 0.1, 7L);
        double mean = 0.0;
        for (double v : samples) {
            mean += v;
            assertThat(v).isBetween(0.0, 1.0);
        }
        assertThat(mean / samples.length).isCloseTo(0.5, within(0.05));
    }

    @Test
    void shouldBeDeterministicPerSeed() {
        DoubleUnaryOperator normalLogDensity = x -> -0.5 * x * x;
        double[] first = SliceSampler.sample(normalLogDensity, 0.0, 100, 1.0, 99L);
        double[] second = SliceSampler.sample(normalLogDensity, 0.0, 100, 1.0, 99L);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldFailFastOnContractViolations() {
        DoubleUnaryOperator normalLogDensity = x -> -0.5 * x * x;
        assertThatThrownBy(() -> SliceSampler.sample(null, 0.0, 10, 1.0, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SliceSampler.sample(normalLogDensity, 0.0, 0, 1.0, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SliceSampler.sample(normalLogDensity, 0.0, 10, 0.0, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SliceSampler.sample(normalLogDensity, Double.NaN, 10, 1.0, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
