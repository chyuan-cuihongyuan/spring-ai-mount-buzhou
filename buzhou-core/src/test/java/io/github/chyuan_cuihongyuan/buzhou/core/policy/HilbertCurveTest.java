package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6017：HilbertCurve 合同——二维网格与一维索引双射+
 * 局部性保持。全格双射遍历钉住；端点锚；单位步索引差
 * 有界；fail-fast。
 */
class HilbertCurveTest {

    @Test
    void bijectionOverAllCellsAtOrder4() {
        int order = 4;
        long side = 1L << order;
        boolean[] seen = new boolean[(int) (side * side)];
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                long d = HilbertCurve.index(x, y, order);
                assertThat(d).as("index(%d,%d)", x, y).isBetween(0L, side * side - 1);
                assertThat(seen[(int) d]).as("双射冲突 (%d,%d)", x, y).isFalse();
                seen[(int) d] = true;
                long[] back = HilbertCurve.coordinate(d, order);
                assertThat(back).containsExactly(x, y);
            }
        }
    }

    @Test
    void curveEndpointsAnchor() {
        int order = 3;
        long side = 1L << order;
        assertThat(HilbertCurve.index(0, 0, order)).isZero();
        assertThat(HilbertCurve.index(side - 1, 0, order)).isEqualTo(side * side - 1);
        assertThat(HilbertCurve.coordinate(0, order)).containsExactly(0L, 0L);
        assertThat(HilbertCurve.coordinate(side * side - 1, order))
                .containsExactly(side - 1, 0L);
    }

    @Test
    void unitStepsHaveBoundedIndexDelta() {
        int order = 6;
        long side = 1L << order;
        long total = side * side;
        long sum = 0;
        int samples = 0;
        for (long y = 0; y < side; y++) {
            for (long x = 0; x + 1 < side; x++) {
                long d1 = HilbertCurve.index(x, y, order);
                long d2 = HilbertCurve.index(x + 1, y, order);
                sum += Math.abs(d2 - d1);
                samples++;
            }
        }
        long average = sum / samples;
        assertThat(average)
                .as("单位步平均索引差 %d 应远小于全域 %d（局部性）", average, total)
                .isLessThan(total / 4);
    }

    @Test
    void order1ShapeIsFixed() {
        assertThat(HilbertCurve.coordinate(0, 1)).containsExactly(0L, 0L);
        assertThat(HilbertCurve.coordinate(1, 1)).containsExactly(0L, 1L);
        assertThat(HilbertCurve.coordinate(2, 1)).containsExactly(1L, 1L);
        assertThat(HilbertCurve.coordinate(3, 1)).containsExactly(1L, 0L);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> HilbertCurve.index(0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HilbertCurve.index(0, 0, 32))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HilbertCurve.index(2, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HilbertCurve.coordinate(-1, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HilbertCurve.coordinate(64, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
