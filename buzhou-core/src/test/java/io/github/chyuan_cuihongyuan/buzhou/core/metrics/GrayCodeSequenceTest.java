package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7039：GrayCodeSequence 合同——二进制反射格雷码。
 * 手锚序列；相邻恰一位差性质；encode/decode 互逆；边界；
 * fail-fast。
 */
class GrayCodeSequenceTest {

    @Test
    void handAnchoredSequence() {
        assertThat(GrayCodeSequence.sequence(3)).containsExactly(0L, 1L, 3L, 2L, 6L, 7L, 5L, 4L);
        assertThat(GrayCodeSequence.sequence(2)).containsExactly(0L, 1L, 3L, 2L);
        assertThat(GrayCodeSequence.encode(0)).isZero();
        assertThat(GrayCodeSequence.encode(1)).isEqualTo(1L);
        assertThat(GrayCodeSequence.encode(2)).isEqualTo(3L);
        assertThat(GrayCodeSequence.encode(3)).isEqualTo(2L);
    }

    @Test
    void adjacentDifferBySingleBitProperty() {
        List<Long> codes = GrayCodeSequence.sequence(10);
        for (int i = 1; i < codes.size(); i++) {
            long diff = codes.get(i) ^ codes.get(i - 1);
            assertThat(Long.bitCount(diff))
                    .as("相邻 %d/%d 恰一位差", i - 1, i).isEqualTo(1);
        }
    }

    @Test
    void encodeDecodeInverse() {
        for (long i = 0; i < 4096; i++) {
            assertThat(GrayCodeSequence.decode(GrayCodeSequence.encode(i))).isEqualTo(i);
        }
        assertThatThrownBy(() -> GrayCodeSequence.encode(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GrayCodeSequence.decode(-2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GrayCodeSequence.sequence(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GrayCodeSequence.sequence(31)).isInstanceOf(IllegalArgumentException.class);
    }
}
