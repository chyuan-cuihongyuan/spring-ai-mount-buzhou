package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5044 / T6190：线段树合同——建树全序、区间和暴力
 * 圣像、点更新回填、单元素、fail-fast。
 */
class SegmentTreeTest {

    private static final int ELEMENT_COUNT = 16;

    private static long[] sampleValues() {
        long[] values = new long[ELEMENT_COUNT];
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            values[i] = (i * 37L) % 23 - 5;
        }
        return values;
    }

    private static long bruteSum(long[] values, int from, int to) {
        long sum = 0;
        for (int i = from; i <= to; i++) {
            sum += values[i];
        }
        return sum;
    }

    @Test
    void rangeSumsShouldMatchBruteForceOracle() {
        long[] values = sampleValues();
        SegmentTree tree = new SegmentTree(values);
        assertThat(tree.size()).isEqualTo(ELEMENT_COUNT);
        for (int from = 0; from < ELEMENT_COUNT; from++) {
            for (int to = from; to < ELEMENT_COUNT; to++) {
                assertThat(tree.rangeSum(from, to))
                        .as("区间 [%d,%d]", from, to)
                        .isEqualTo(bruteSum(values, from, to));
            }
        }
    }

    @Test
    void pointUpdateShouldReflowAncestors() {
        long[] values = sampleValues();
        SegmentTree tree = new SegmentTree(values);
        tree.update(5, 100);
        values[5] = 100;
        tree.update(0, -7);
        values[0] = -7;
        assertThat(tree.rangeSum(0, ELEMENT_COUNT - 1))
                .isEqualTo(bruteSum(values, 0, ELEMENT_COUNT - 1));
        assertThat(tree.rangeSum(4, 8)).isEqualTo(bruteSum(values, 4, 8));
        assertThat(tree.rangeSum(5, 5)).isEqualTo(100);
    }

    @Test
    void singleElementShouldWork() {
        SegmentTree tree = new SegmentTree(new long[]{42});
        assertThat(tree.rangeSum(0, 0)).isEqualTo(42);
        tree.update(0, 7);
        assertThat(tree.rangeSum(0, 0)).isEqualTo(7);
    }

    @Test
    void negativeValuesShouldSumCorrectly() {
        SegmentTree tree = new SegmentTree(new long[]{-3, -4, 10, -1});
        assertThat(tree.rangeSum(0, 3)).isEqualTo(2);
        assertThat(tree.rangeSum(0, 1)).isEqualTo(-7);
        assertThat(tree.rangeSum(2, 3)).isEqualTo(9);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new SegmentTree(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SegmentTree(new long[]{})).isInstanceOf(IllegalArgumentException.class);
        SegmentTree tree = new SegmentTree(new long[]{1, 2, 3});
        assertThatThrownBy(() -> tree.rangeSum(-1, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.rangeSum(0, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.rangeSum(2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.update(3, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.update(-1, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
