package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class RejectionSamplerTest {

    /** 三角密度（[0,1] 上 f(x)=2−|4x−2|，峰值 2 于中点）。 */
    private static double triangular(double x) {
        return 2 - Math.abs(4 * x - 2);
    }

    @Test
    void shouldSampleTriangularDensitySymmetrically() {
        RejectionSampler sampler = RejectionSampler.of(RejectionSamplerTest::triangular, 0, 1, 2.0, 42);
        int samples = 20000;
        long leftSum = 0;
        long leftCount = 0;
        long count = 0;
        double sum = 0;
        for (int i = 0; i < samples; i++) {
            double value = sampler.sample();
            sum += value;
            count++;
            if (value < 0.5) {
                leftSum += (long) (value * 1000);
                leftCount++;
            }
        }
        assertThat(count).isEqualTo(samples);
        double mean = sum / samples;
        assertThat(mean).isCloseTo(0.5, within(0.02));
        double leftMean = (double) leftSum / Math.max(1, leftCount) / 1000;
        assertThat(leftMean).isCloseTo(1.0 / 3.0, within(0.05));
    }

    @Test
    void shouldBeSeedDeterministic() {
        RejectionSampler first = RejectionSampler.of(x -> 1, 0, 1, 1.5, 99);
        RejectionSampler second = RejectionSampler.of(x -> 1, 0, 1, 1.5, 99);
        for (int i = 0; i < 50; i++) {
            assertThat(first.sample()).isEqualTo(second.sample());
        }
    }

    @Test
    void shouldFailFastOnBadConstructionAndDeficientEnvelope() {
        assertThatThrownBy(() -> RejectionSampler.of(null, 0, 1, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RejectionSampler.of(x -> 1, 1, 1, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RejectionSampler.of(x -> 1, 0, 1, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        RejectionSampler deficient = RejectionSampler.of(RejectionSamplerTest::triangular, 0, 1, 1.0, 1);
        assertThatThrownBy(deficient::sample)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("包络不足");
        assertThatThrownBy(() -> RejectionSampler.of(x -> -1, 0, 1, 1, 1).sample())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("密度非负");
    }
}
