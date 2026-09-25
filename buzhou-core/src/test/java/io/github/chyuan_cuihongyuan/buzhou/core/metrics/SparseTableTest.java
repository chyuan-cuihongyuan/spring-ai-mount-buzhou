package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6003 / T6208：SparseTable 合同——静态序列 O(1) 区间
 * 最小值。暴力线性扫圣像全等（全子区间 + 随机大样本）；
 * 重复/负值/单元素边界；fail-fast。
 */
class SparseTableTest {

    private static long bruteMin(long[] a, int from, int to) {
        long min = a[from];
        for (int i = from + 1; i <= to; i++) {
            min = Math.min(min, a[i]);
        }
        return min;
    }

    @Test
    void allSubrangesMatchBruteForce() {
        long[] a = {7, 3, 5, 3, -1, 8, 8, 2, 9, 0, -5, 4, 4, 6, 1, 1, 2};
        SparseTable table = new SparseTable(a);
        assertThat(table.size()).isEqualTo(17);
        for (int i = 0; i < a.length; i++) {
            for (int j = i; j < a.length; j++) {
                assertThat(table.rangeMin(i, j)).as("[%d,%d]", i, j).isEqualTo(bruteMin(a, i, j));
            }
        }
    }

    @Test
    void largeRandomQueriesMatchBruteForce() {
        long[] a = new long[1000];
        Random rng = new Random(6003L);
        for (int i = 0; i < a.length; i++) {
            a[i] = rng.nextLong(1_000_000) - 500_000;
        }
        SparseTable table = new SparseTable(a);
        for (int q = 0; q < 1000; q++) {
            int i = rng.nextInt(a.length);
            int j = i + rng.nextInt(a.length - i);
            assertThat(table.rangeMin(i, j)).as("[%d,%d]", i, j).isEqualTo(bruteMin(a, i, j));
        }
    }

    @Test
    void singleElementAndDuplicateBoundaries() {
        SparseTable single = new SparseTable(new long[]{42});
        assertThat(single.rangeMin(0, 0)).isEqualTo(42);
        SparseTable dup = new SparseTable(new long[]{5, 5, 5, 5});
        assertThat(dup.rangeMin(0, 3)).isEqualTo(5);
        assertThat(dup.rangeMin(1, 2)).isEqualTo(5);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new SparseTable(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SparseTable(new long[0])).isInstanceOf(IllegalArgumentException.class);
        SparseTable table = new SparseTable(new long[]{1, 2, 3});
        assertThatThrownBy(() -> table.rangeMin(2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.rangeMin(-1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.rangeMin(0, 3)).isInstanceOf(IllegalArgumentException.class);
    }
}
