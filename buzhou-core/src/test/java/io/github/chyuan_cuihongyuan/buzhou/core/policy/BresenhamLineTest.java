package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 7014：BresenhamLine 合同——整数光栅走格。端点含、
 * 格数 = max(|dx|,|dy|)+1、逐格步进 ≤1、双向对称、轴对齐；
 * 随机线性质钉住。
 */
class BresenhamLineTest {

    @Test
    void axisAlignedAndDiagonalAnchors() {
        List<long[]> horizontal = BresenhamLine.line(0, 0, 3, 0);
        assertThat(horizontal).hasSize(4);
        assertThat(horizontal.get(3)).containsExactly(3L, 0L);

        List<long[]> vertical = BresenhamLine.line(2, 5, 2, 1);
        assertThat(vertical).hasSize(5);
        assertThat(vertical.get(0)).containsExactly(2L, 5L);
        assertThat(vertical.get(4)).containsExactly(2L, 1L);

        List<long[]> diagonal = BresenhamLine.line(0, 0, 3, 3);
        assertThat(diagonal).hasSize(4);
        assertThat(diagonal.get(2)).containsExactly(2L, 2L);
    }

    @Test
    void shallowSlopeStaircase() {
        List<long[]> cells = BresenhamLine.line(0, 0, 5, 2);
        assertThat(cells).hasSize(6);
        assertThat(cells.get(5)).containsExactly(5L, 2L);
        for (int i = 1; i < cells.size(); i++) {
            long dx = cells.get(i)[0] - cells.get(i - 1)[0];
            long dy = cells.get(i)[1] - cells.get(i - 1)[1];
            assertThat(Math.abs(dx)).isLessThanOrEqualTo(1);
            assertThat(Math.abs(dy)).isLessThanOrEqualTo(1);
        }
    }

    @Test
    void randomLinesPropertyHold() {
        Random rng = new Random(7014L);
        for (int round = 0; round < 300; round++) {
            int x0 = rng.nextInt(41) - 20;
            int y0 = rng.nextInt(41) - 20;
            int x1 = rng.nextInt(41) - 20;
            int y1 = rng.nextInt(41) - 20;
            List<long[]> forward = BresenhamLine.line(x0, y0, x1, y1);
            assertThat(forward.get(0)).containsExactly((long) x0, (long) y0);
            assertThat(forward.get(forward.size() - 1))
                    .containsExactly((long) x1, (long) y1);
            int expected = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) + 1;
            assertThat(forward).as("(%d,%d)→(%d,%d)", x0, y0, x1, y1).hasSize(expected);
            List<long[]> backward = BresenhamLine.line(x1, y1, x0, y0);
            assertThat(backward).as("反向同样的格数").hasSize(expected);
            assertThat(backward.get(0)).containsExactly((long) x1, (long) y1);
        }
    }
}
