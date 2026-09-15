package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1869 / T2940：流式中位数——奇偶、乱序流、空哨兵。 */
class MedianKeeperTest {

    /** 奇数取小半堆顶；偶数取两堆顶均值。 */
    @Test
    void shouldTrackMedianOddAndEven() {
        MedianKeeper keeper = new MedianKeeper();
        keeper.add(5);
        assertThat(keeper.median()).isEqualTo(5d);
        keeper.add(1);
        assertThat(keeper.median()).isEqualTo(3d); // (1+5)/2
        keeper.add(9);
        assertThat(keeper.median()).isEqualTo(5d); // {1,5,9}
        assertThat(keeper.size()).isEqualTo(3);
    }

    /** 乱序流与排序流同中位（流式性质——与到达序无关）。 */
    @Test
    void medianIsArrivalOrderIndependent() {
        MedianKeeper shuffled = new MedianKeeper();
        IntStream.of(7, 2, 9, 4, 3, 8, 1, 6, 5).forEach(shuffled::add);
        assertThat(shuffled.median()).isEqualTo(5d);

        MedianKeeper sorted = new MedianKeeper();
        IntStream.rangeClosed(1, 9).forEach(i -> sorted.add(i));
        assertThat(sorted.median()).isEqualTo(5d).isEqualTo(shuffled.median());
    }

    /** 极值偏置流：单侧长尾不拉走中位（对照均值被拉爆）。 */
    @Test
    void medianResistsSkew() {
        MedianKeeper keeper = new MedianKeeper();
        IntStream.rangeClosed(1, 9).forEach(keeper::add);
        keeper.add(1_000_000);
        keeper.add(2_000_000);
        // 11 样本中位仍是 6
        assertThat(keeper.median()).isEqualTo(6d);
    }

    /** 空哨兵与畸形 fail-fast。 */
    @Test
    void emptySentinelAndMalformed() {
        assertThat(new MedianKeeper().median()).isEqualTo(-1d);
        assertThat(new MedianKeeper().size()).isZero();
        MedianKeeper keeper = new MedianKeeper();
        assertThatThrownBy(() -> keeper.add(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("有限实数");
        assertThatThrownBy(() -> keeper.add(Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
