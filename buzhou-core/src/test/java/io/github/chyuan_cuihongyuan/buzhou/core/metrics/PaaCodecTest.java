package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaaCodecTest {

    @Test
    void shouldComputeExactMeansOnDivisibleLengths() {
        assertThat(PaaCodec.transform(new double[]{1, 2, 3, 4}, 2)).containsExactly(1.5, 3.5);
        assertThat(PaaCodec.transform(new double[]{1, 2, 3, 4}, 4)).containsExactly(1, 2, 3, 4);
        assertThat(PaaCodec.transform(new double[]{1, 2, 3, 4}, 1)).containsExactly(2.5);
        assertThat(PaaCodec.transform(new double[]{3}, 1)).containsExactly(3.0);
    }

    @Test
    void shouldFoldRemainderIntoLastSegment() {
        double[] reduced = PaaCodec.transform(new double[]{1, 2, 3, 4, 5}, 2);
        assertThat(reduced).hasSize(2);
        assertThat(reduced[0]).isEqualTo(1.5);
        assertThat(reduced[1]).isEqualTo(4.0);
    }

    @Test
    void shouldMatchSegmentMeanOracleOnRandomInputs() {
        Random random = new Random(8047);
        for (int round = 0; round < 300; round++) {
            int length = 1 + random.nextInt(30);
            double[] series = new double[length];
            for (int i = 0; i < length; i++) {
                series[i] = random.nextGaussian();
            }
            int segments = 1 + random.nextInt(length);
            double[] reduced = PaaCodec.transform(series, segments);
            assertThat(reduced).hasSize(segments);
            for (int segment = 0; segment < segments; segment++) {
                int from = (int) Math.floor((double) segment * length / segments);
                int to = segment == segments - 1 ? length
                        : (int) Math.floor((double) (segment + 1) * length / segments);
                double sum = 0;
                for (int i = from; i < to; i++) {
                    sum += series[i];
                }
                assertThat(reduced[segment]).as("round=%d segment=%d", round, segment)
                        .isEqualTo(sum / (to - from));
            }
        }
    }

    @Test
    void shouldFailFastOnBadInputs() {
        assertThatThrownBy(() -> PaaCodec.transform(null, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PaaCodec.transform(new double[0], 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PaaCodec.transform(new double[]{1}, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PaaCodec.transform(new double[]{1}, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
