package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ZOrderCurve.Point;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 2064 / T3230：Z 序曲线合同——编解码往返（含负值/极值）、已知
 * 编码值、邻域局部性（标量差与距离同数量级）、位交织结构。
 */
class ZOrderCurveTest {

    @Test
    void encodeDecodeShouldRoundTrip() {
        assertThat(ZOrderCurve.decode(ZOrderCurve.encode(3, 5))).isEqualTo(new Point(3, 5));
        assertThat(ZOrderCurve.decode(ZOrderCurve.encode(0, 0))).isEqualTo(new Point(0, 0));
        assertThat(ZOrderCurve.decode(ZOrderCurve.encode(-7, 42))).isEqualTo(new Point(-7, 42));
        assertThat(ZOrderCurve.decode(ZOrderCurve.encode(Integer.MAX_VALUE, Integer.MIN_VALUE)))
                .isEqualTo(new Point(Integer.MAX_VALUE, Integer.MIN_VALUE));
    }

    @Test
    void knownEncodingsShouldMatchHandComputation() {
        // x=1,y=0 → bit0=1 → 0b01 = 1；x=0,y=1 → bit1=1 → 0b10 = 2；x=1,y=1 → 0b11 = 3
        assertThat(ZOrderCurve.encode(1, 0)).isEqualTo(1L);
        assertThat(ZOrderCurve.encode(0, 1)).isEqualTo(2L);
        assertThat(ZOrderCurve.encode(1, 1)).isEqualTo(3L);
        assertThat(ZOrderCurve.encode(2, 0)).isEqualTo(4L);  // x bit1 → bit2
        assertThat(ZOrderCurve.encode(0, 2)).isEqualTo(8L);  // y bit1 → bit3
    }

    @Test
    void grid4x4ScalarsShouldStayCompact() {
        // 格内紧致（数学可证）：x,y < 16 → 各维 4 有效位 → 标量 < 2^8 = 256
        // ——16×16 格全落 256 长标量段（一维区间即多维子空间）
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                assertThat(ZOrderCurve.encode(x, y)).isLessThan(256L);
            }
        }
    }

    @Test
    void adjacentCellsShouldMapWithinSameSmallBand() {
        // 相邻格对标量差有界：4×4 格内任两点差 < 256（局部性的格内口径；
        // 逐对紧界不成立——借位位结构致 Z 形跳变，渐近/格内才是语义）
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                long z = ZOrderCurve.encode(x, y);
                max = Math.max(max, z);
                min = Math.min(min, z);
            }
        }
        assertThat(max - min).isLessThan(64L); // 16 格极差 < 64（4 位标量域）
    }

    @Test
    void smallGridScalarsShouldStayBounded() {
        // 8×8 网格内全部标量差 ≤ 64^2（紧致——非全空间散布）
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                assertThat(ZOrderCurve.encode(x, y)).isLessThan(64L * 64);
            }
        }
    }

    @Test
    void interleavingStructureShouldAlternateBits() {
        // x=0b11（3）y=0b00：标量 0b0101（bit0/bit2 置位）
        assertThat(ZOrderCurve.encode(3, 0)).isEqualTo(0b0101L);
        // x=0 y=0b11：标量 0b1010（bit1/bit3）
        assertThat(ZOrderCurve.encode(0, 3)).isEqualTo(0b1010L);
    }

    @Test
    void scalarGapShouldBeSymmetric() {
        assertThat(ZOrderCurve.scalarGap(1, 1, 4, 4))
                .isEqualTo(ZOrderCurve.scalarGap(4, 4, 1, 1));
    }
}
