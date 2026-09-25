package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6015：Simple8b 合同——60 位载荷 16 档变长打包。
 * 混合量级往返全等；全零档压缩钉住；解码字数守恒；确定性；
 * fail-fast。
 */
class Simple8bTest {

    @Test
    void mixedMagnitudeRoundTrip() {
        Random rng = new Random(6015L);
        long[] values = new long[500];
        for (int i = 0; i < values.length; i++) {
            int bucket = rng.nextInt(4);
            values[i] = switch (bucket) {
                case 0 -> 0;
                case 1 -> rng.nextInt(16);
                case 2 -> rng.nextLong(1L << 20);
                default -> rng.nextLong(1L << 59);
            };
        }
        Simple8b encoded = Simple8b.encode(values);
        assertThat(encoded.decode()).containsExactly(values);
        assertThat(encoded.valueCount()).isEqualTo(values.length);
    }

    @Test
    void allZerosPackDensely() {
        long[] zeros = new long[960];
        Simple8b encoded = Simple8b.encode(zeros);
        assertThat(encoded.wordCount()).as("240 值/字 → 960 零恰 4 字").isEqualTo(4);
        assertThat(encoded.decodeWord(encoded.wordsCopy()[0])).hasSize(240);
        assertThat(encoded.decode()).containsExactly(zeros);
        long[] tail = new long[100];
        assertThat(Simple8b.encode(tail).wordCount()).as("尾零截断至 1 字").isEqualTo(1);
    }

    @Test
    void singleWideValueOccupiesFullWord() {
        long[] values = {0, (1L << 59) - 1};
        Simple8b encoded = Simple8b.encode(values);
        assertThat(encoded.decode()).containsExactly(values);
        long wide = (1L << 60) - 1;
        Simple8b wideOnly = Simple8b.encode(new long[]{wide});
        assertThat(wideOnly.wordCount()).isEqualTo(1);
        assertThat((wideOnly.wordsCopy()[0] >>> 60)).isEqualTo(15);
        assertThat(wideOnly.decode()).containsExactly(wide);
    }

    @Test
    void deterministicWordStream() {
        long[] values = {1, 2, 3, 0, 0, 0, 7, 1L << 20};
        assertThat(Simple8b.encode(values).wordsCopy())
                .containsExactly(Simple8b.encode(values).wordsCopy());
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> Simple8b.encode(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Simple8b.encode(new long[]{-1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Simple8b.encode(new long[]{1L << 60}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Simple8b.encode(new long[0]).wordCount()).isZero();
        assertThat(Simple8b.decodeWord(0L)).hasSize(240).containsOnly(0);
    }
}
