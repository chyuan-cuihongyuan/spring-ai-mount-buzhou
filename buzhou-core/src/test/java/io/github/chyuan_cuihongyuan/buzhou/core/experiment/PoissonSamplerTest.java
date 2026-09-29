package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoissonSamplerTest {

    @Test
    void shouldHaveCorrectModeAtLambdaOne() {
        Random random = new Random(8026);
        int zeros = 0;
        int ones = 0;
        int samples = 5000;
        for (int i = 0; i < samples; i++) {
            int value = PoissonSampler.sample(1.0, random);
            if (value == 0) {
                zeros++;
            } else if (value == 1) {
                ones++;
            }
        }
        double zeroRatio = (double) zeros / samples;
        double oneRatio = (double) ones / samples;
        assertThat(zeroRatio).isBetween(0.30, 0.40);
        assertThat(oneRatio).isBetween(0.30, 0.40);
    }

    @Test
    void shouldMatchMeanAndVarianceWithinTolerance() {
        Random random = new Random(8027);
        double lambda = 4.0;
        int samples = 6000;
        long sum = 0;
        long squareSum = 0;
        for (int i = 0; i < samples; i++) {
            int value = PoissonSampler.sample(lambda, random);
            sum += value;
            squareSum += (long) value * value;
        }
        double mean = (double) sum / samples;
        double variance = (double) squareSum / samples - mean * mean;
        assertThat(mean).isBetween(lambda - 0.15, lambda + 0.15);
        assertThat(variance).isBetween(lambda - 0.6, lambda + 0.6);
    }

    @Test
    void shouldBeSeedDeterministicAndFailFast() {
        Random first = new Random(99);
        Random second = new Random(99);
        for (int i = 0; i < 50; i++) {
            assertThat(PoissonSampler.sample(2.5, first))
                    .isEqualTo(PoissonSampler.sample(2.5, second));
        }
        assertThatThrownBy(() -> PoissonSampler.sample(0, new Random(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PoissonSampler.sample(-1, new Random(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PoissonSampler.sample(1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
