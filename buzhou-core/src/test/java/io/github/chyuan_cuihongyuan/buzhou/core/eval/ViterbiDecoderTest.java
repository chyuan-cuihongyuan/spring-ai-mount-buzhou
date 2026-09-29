package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ViterbiDecoderTest {

    private static final double[] INITIAL = {0.6, 0.4};
    private static final double[][] TRANSITIONS = {{0.7, 0.3}, {0.4, 0.6}};
    private static final double[][] EMISSIONS = {
            {0.9, 0.2},
            {0.1, 0.8}};

    @Test
    void shouldDecodeWeatherUmbrellaClassic() {
        double[] initial = {0.5, 0.5};
        double[][] transitions = {{0.8, 0.2}, {0.4, 0.6}};
        double[][] emissions = {
                {0.8, 0.2},
                {0.8, 0.2},
                {0.05, 0.95}};
        int[] path = ViterbiDecoder.decode(initial, transitions, emissions);
        assertThat(path).containsExactly(0, 0, 1);
    }

    @Test
    void shouldMatchBruteForceOracleOnSmallDomain() {
        double[][] emissions = {
                {0.8, 0.2},
                {0.3, 0.7},
                {0.5, 0.5},
                {0.1, 0.9}};
        int[] path = ViterbiDecoder.decode(INITIAL, TRANSITIONS, emissions);
        double best = -1;
        int[] expected = new int[emissions.length];
        for (int s0 = 0; s0 < 2; s0++) {
            for (int s1 = 0; s1 < 2; s1++) {
                for (int s2 = 0; s2 < 2; s2++) {
                    for (int s3 = 0; s3 < 2; s3++) {
                        double probability = INITIAL[s0] * emissions[0][s0]
                                * TRANSITIONS[s0][s1] * emissions[1][s1]
                                * TRANSITIONS[s1][s2] * emissions[2][s2]
                                * TRANSITIONS[s2][s3] * emissions[3][s3];
                        if (probability > best) {
                            best = probability;
                            expected = new int[]{s0, s1, s2, s3};
                        }
                    }
                }
            }
        }
        assertThat(path).containsExactly(expected);
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        double[][] emissions = {{0.9, 0.1}, {0.2, 0.8}};
        int[] first = ViterbiDecoder.decode(INITIAL, TRANSITIONS, emissions);
        int[] second = ViterbiDecoder.decode(INITIAL, TRANSITIONS, emissions);
        assertThat(first).isEqualTo(second);
        assertThatThrownBy(() -> ViterbiDecoder.decode(null, TRANSITIONS, emissions))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ViterbiDecoder.decode(new double[]{0.5}, TRANSITIONS, emissions))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ViterbiDecoder.decode(new double[]{0.5, 0.6}, TRANSITIONS, emissions))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ViterbiDecoder.decode(new double[]{0.5, 0.5},
                new double[][]{{0.9, 0.2}, {0.4, 0.6}}, emissions))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ViterbiDecoder.decode(INITIAL, TRANSITIONS, new double[0][]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
