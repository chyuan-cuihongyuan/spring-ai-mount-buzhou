package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class AliasMethodTest {

    @Test
    void shouldSampleUniformlyForEqualWeights() {
        AliasMethod method = AliasMethod.of(new double[]{1, 1, 1, 1}, 42);
        int[] counts = new int[4];
        for (int i = 0; i < 4000; i++) {
            counts[method.sample()]++;
        }
        assertThat(counts[0]).isBetween(850, 1150);
        assertThat(counts[3]).isBetween(850, 1150);
        assertThat(method.totalWeight()).isCloseTo(4.0, within(1e-9));
    }

    @Test
    void shouldMatchNormalizedWeightsWithinTolerance() {
        Random random = new Random(8025);
        double[] weights = {random.nextDouble() + 0.1, random.nextDouble() + 0.1,
                random.nextDouble() + 0.1, random.nextDouble() + 0.1, random.nextDouble() + 0.1};
        double total = 0;
        for (double weight : weights) {
            total += weight;
        }
        AliasMethod method = AliasMethod.of(weights, 7);
        int[] counts = new int[weights.length];
        int samples = 30000;
        for (int i = 0; i < samples; i++) {
            counts[method.sample()]++;
        }
        for (int i = 0; i < weights.length; i++) {
            double expectedRatio = weights[i] / total;
            double actualRatio = (double) counts[i] / samples;
            assertThat(actualRatio).as("bucket %d", i).isCloseTo(expectedRatio, within(0.03));
        }
    }

    @Test
    void shouldBeSeedDeterministicAndFailFast() {
        double[] weights = {2, 1, 3};
        AliasMethod first = AliasMethod.of(weights, 99);
        AliasMethod second = AliasMethod.of(weights, 99);
        for (int i = 0; i < 50; i++) {
            assertThat(first.sample()).isEqualTo(second.sample());
        }
        assertThatThrownBy(() -> AliasMethod.of(null, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AliasMethod.of(new double[0], 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AliasMethod.of(new double[]{1, -1}, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AliasMethod.of(new double[]{0, 0}, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
