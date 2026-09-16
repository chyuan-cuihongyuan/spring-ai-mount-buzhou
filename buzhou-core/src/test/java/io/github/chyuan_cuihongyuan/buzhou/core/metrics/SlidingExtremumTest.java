package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2061 / T3224：单调队列滑窗极值合同——max/min 双口径、窗口滑出、
 * 压制弹出 O(1) 摊销语义、空窗 NaN、畸形 fail-fast。
 */
class SlidingExtremumTest {

    @Test
    void slidingMaxShouldTrackWindow() {
        SlidingExtremum max = new SlidingExtremum(3, true);
        assertThat(max.offer(1)).isEqualTo(1.0d);   // [1]
        assertThat(max.offer(3)).isEqualTo(3.0d);   // [1,3]
        assertThat(max.offer(2)).isEqualTo(3.0d);   // [1,3,2]
        assertThat(max.offer(1)).isEqualTo(3.0d);   // [3,2,1]（3 未过期）
        assertThat(max.offer(0)).isEqualTo(2.0d);   // [2,1,0]（3 滑出）
        assertThat(max.offer(5)).isEqualTo(5.0d);   // [1,0,5]→压制后 [5]
    }

    @Test
    void slidingMinShouldMirrorMax() {
        SlidingExtremum min = new SlidingExtremum(3, false);
        assertThat(min.offer(5)).isEqualTo(5.0d);
        assertThat(min.offer(2)).isEqualTo(2.0d);
        assertThat(min.offer(8)).isEqualTo(2.0d);
        assertThat(min.offer(9)).isEqualTo(2.0d);   // [2,8,9]
        assertThat(min.offer(1)).isEqualTo(1.0d);   // [9,1]→压制 [1]
    }

    @Test
    void newMaxShouldSuppressSmallerTail() {
        SlidingExtremum max = new SlidingExtremum(10, true);
        max.offer(1);
        max.offer(2);
        assertThat(max.size()).isEqualTo(1); // 递增入队即压制——1≤2 已弹，只 [2]
        max.offer(3);
        assertThat(max.size()).isEqualTo(1); // 递增入队逐个压制——只剩 3（队内候选=潜在未来 max）
        max.offer(10); // 压制全部
        assertThat(max.size()).isEqualTo(1); // 队内只剩 10
        assertThat(max.current()).isEqualTo(10.0d);
    }

    @Test
    void decreasingStreamMaxShouldStayFresh() {
        SlidingExtremum max = new SlidingExtremum(2, true);
        assertThat(max.offer(9)).isEqualTo(9.0d);
        assertThat(max.offer(5)).isEqualTo(9.0d);   // [9,5]
        assertThat(max.offer(3)).isEqualTo(5.0d);   // 9 滑出 [5,3]
        assertThat(max.offer(1)).isEqualTo(3.0d);
    }

    @Test
    void emptyWindowShouldReturnNaN() {
        assertThat(new SlidingExtremum(3, true).current()).isNaN();
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new SlidingExtremum(0, true))
                .isInstanceOf(IllegalArgumentException.class);
        SlidingExtremum max = new SlidingExtremum(3, true);
        assertThatThrownBy(() -> max.offer(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> max.offer(Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
