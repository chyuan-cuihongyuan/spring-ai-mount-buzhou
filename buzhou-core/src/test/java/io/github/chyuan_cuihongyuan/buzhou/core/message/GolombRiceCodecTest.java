package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7020：GolombRiceCodec 合同——商 unary+余数定宽。
 * 手算向量；随机 roundtrip（多 k 档）；小值偏斜压缩；
 * fail-fast。
 */
class GolombRiceCodecTest {

    @Test
    void handComputedVector() {
        GolombRiceCodec.BitStream stream = GolombRiceCodec.encode(new long[]{13}, 2);
        assertThat(stream.bitLength()).isEqualTo(6);
        assertThat(stream.bytes()).containsExactly((byte) 0b11100100);
        assertThat(GolombRiceCodec.decode(stream, 2, 1)).containsExactly(13L);
    }

    @Test
    void zeroAndMaxQuotientRoundTrip() {
        GolombRiceCodec.BitStream zeros = GolombRiceCodec.encode(new long[]{0, 0, 0}, 3);
        assertThat(GolombRiceCodec.decode(zeros, 3, 3)).containsExactly(0L, 0L, 0L);
        long big = (1L << 20) + 5;
        GolombRiceCodec.BitStream stream = GolombRiceCodec.encode(new long[]{big}, 5);
        assertThat(GolombRiceCodec.decode(stream, 5, 1)).containsExactly(big);
    }

    @Test
    void randomRoundTripAcrossParams() {
        Random rng = new Random(7019L);
        for (int k = 0; k <= 6; k++) {
            long[] values = new long[200];
            for (int i = 0; i < values.length; i++) {
                values[i] = rng.nextLong(Math.max(1L, 1L << (k + 4)));
            }
            GolombRiceCodec.BitStream stream = GolombRiceCodec.encode(values, k);
            assertThat(GolombRiceCodec.decode(stream, k, values.length))
                    .as("k=%d", k).containsExactly(values);
        }
    }

    @Test
    void skewedSmallValuesCompress() {
        long[] skewed = {0, 1, 0, 2, 1, 0, 3, 1};
        GolombRiceCodec.BitStream stream = GolombRiceCodec.encode(skewed, 1);
        assertThat(stream.bitLength()).isLessThanOrEqualTo(skewed.length * 8);
        assertThat(GolombRiceCodec.decode(stream, 1, skewed.length)).containsExactly(skewed);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> GolombRiceCodec.encode(null, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GolombRiceCodec.encode(new long[]{-1}, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GolombRiceCodec.encode(new long[]{1}, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GolombRiceCodec.encode(new long[]{1}, 31)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GolombRiceCodec.decode(null, 2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GolombRiceCodec.decode(
                GolombRiceCodec.encode(new long[]{0}, 2), 2, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
