package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6016：BitPacking 合同——固定位宽无缝串接打包。
 * 多位宽往返全等（含跨字取值）；字数公式钉住；fail-fast。
 */
class BitPackingTest {

    @Test
    void roundTripAcrossWidths() {
        Random rng = new Random(6016L);
        for (int width : new int[]{0, 1, 3, 7, 31, 32, 33, 63}) {
            long[] values = new long[100];
            long limit = switch (width) {
                case 0 -> 1;
                case 63 -> Long.MAX_VALUE;
                default -> 1L << width;
            };
            for (int i = 0; i < values.length; i++) {
                values[i] = limit == 1 ? 0 : rng.nextLong(limit);
            }
            BitPacking packed = BitPacking.pack(values, width);
            assertThat(packed.unpack()).as("width=%d", width).containsExactly(values);
            assertThat(packed.bitWidth()).isEqualTo(width);
            assertThat(packed.count()).isEqualTo(100);
        }
    }

    @Test
    void crossWordBoundaryAccess() {
        long[] values = new long[100];
        Random rng = new Random(61L);
        for (int i = 0; i < values.length; i++) {
            values[i] = rng.nextLong(1L << 7);
        }
        BitPacking packed = BitPacking.pack(values, 7);
        assertThat(packed.wordCount()).as("700 位 → 11 字").isEqualTo(11);
        for (int probe = 0; probe < 100; probe++) {
            assertThat(packed.get(probe)).isEqualTo(values[probe]);
        }
    }

    @Test
    void fullWidthStoresAnyNonNegative() {
        long[] values = {0, 1, Long.MAX_VALUE, Long.MAX_VALUE / 2};
        BitPacking packed = BitPacking.pack(values, 64);
        assertThat(packed.unpack()).containsExactly(values);
        assertThat(packed.wordCount()).isEqualTo(4);
    }

    @Test
    void deterministicPackedStream() {
        long[] values = {1, 2, 3, 4, 5, 6, 7};
        assertThat(BitPacking.pack(values, 3).packedCopy())
                .containsExactly(BitPacking.pack(values, 3).packedCopy());
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> BitPacking.pack(null, 8))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BitPacking.pack(new long[]{1}, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BitPacking.pack(new long[]{1}, 65))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BitPacking.pack(new long[]{4}, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BitPacking.pack(new long[]{-1}, 64))
                .isInstanceOf(IllegalArgumentException.class);
        BitPacking packed = BitPacking.pack(new long[]{1, 2}, 3);
        assertThatThrownBy(() -> packed.get(2)).isInstanceOf(IllegalArgumentException.class);
    }
}
