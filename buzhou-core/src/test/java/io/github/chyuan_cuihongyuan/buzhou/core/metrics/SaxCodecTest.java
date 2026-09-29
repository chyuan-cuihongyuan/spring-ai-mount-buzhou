package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaxCodecTest {

    @Test
    void shouldSymbolizeTrendAndConstantHonestly() {
        assertThat(SaxCodec.transform(new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, 3)).isEqualTo("abc");
        assertThat(SaxCodec.transform(new double[]{9, 8, 7, 6, 5, 4, 3, 2, 1}, 3)).isEqualTo("cba");
        assertThat(SaxCodec.transform(new double[]{5, 5, 5}, 3)).isEqualTo("bbb");
        assertThat(SaxCodec.transform(new double[]{1, 2, 3}, 1)).hasSize(1);
        String word = SaxCodec.transform(new double[]{1, 5, 2, 6, 3, 7, 4, 8}, 4);
        for (char symbol : word.toCharArray()) {
            assertThat(symbol).isBetween('a', 'c');
        }
    }

    @Test
    void shouldBeDeterministicOnRandomInputs() {
        Random random = new Random(8046);
        for (int round = 0; round < 300; round++) {
            int length = 2 + random.nextInt(20);
            double[] series = new double[length];
            for (int i = 0; i < length; i++) {
                series[i] = random.nextGaussian();
            }
            int wordSize = 1 + random.nextInt(length);
            String first = SaxCodec.transform(series, wordSize);
            String second = SaxCodec.transform(series, wordSize);
            assertThat(first).as("round=%d", round).isEqualTo(second);
            assertThat(first).hasSize(wordSize);
        }
    }

    @Test
    void shouldFailFastOnBadInputs() {
        assertThatThrownBy(() -> SaxCodec.transform(null, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SaxCodec.transform(new double[0], 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SaxCodec.transform(new double[]{1}, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SaxCodec.transform(new double[]{1}, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
