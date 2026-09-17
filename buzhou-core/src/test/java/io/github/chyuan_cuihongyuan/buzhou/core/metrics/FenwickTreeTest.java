package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3015 / T5032：Fenwick 合同——前缀和手算、负增量回零、区间和
 * = 前缀差、随机点更新对拍朴素数组、边界（空前缀/全前缀）、越界
 * fail-fast、初值全零、long 大值无溢出。
 */
class FenwickTreeTest {

    @Test
    void prefixSumsShouldMatchHandComputation() {
        FenwickTree fenwick = new FenwickTree(8);
        fenwick.add(0, 3);
        fenwick.add(1, 5);
        fenwick.add(2, 7);
        fenwick.add(4, 2);
        assertThat(fenwick.prefixSum(0)).isZero();
        assertThat(fenwick.prefixSum(1)).isEqualTo(3);
        assertThat(fenwick.prefixSum(2)).isEqualTo(8);
        assertThat(fenwick.prefixSum(3)).isEqualTo(15);
        assertThat(fenwick.prefixSum(4)).isEqualTo(15);
        assertThat(fenwick.prefixSum(5)).isEqualTo(17);
        assertThat(fenwick.total()).isEqualTo(17);
    }

    @Test
    void negativeDeltaShouldReturnToZero() {
        FenwickTree fenwick = new FenwickTree(4);
        fenwick.add(2, 100);
        assertThat(fenwick.prefixSum(4)).isEqualTo(100);
        fenwick.add(2, -100);
        assertThat(fenwick.prefixSum(4)).isZero();
    }

    @Test
    void rangeSumShouldBePrefixDifference() {
        FenwickTree fenwick = new FenwickTree(4);
        fenwick.add(0, 3);
        fenwick.add(1, 5);
        fenwick.add(2, 7);
        fenwick.add(3, 2);
        assertThat(fenwick.rangeSum(1, 3)).isEqualTo(12);
        assertThat(fenwick.rangeSum(0, 4)).isEqualTo(17);
        assertThat(fenwick.rangeSum(2, 2)).isZero();
    }

    @Test
    void randomUpdatesShouldAgreeWithNaiveArray() {
        RandomGenerator rng = RandomGeneratorFactory.of("L64X256MixRandom").create(19);
        int size = 64;
        FenwickTree fenwick = new FenwickTree(size);
        long[] naive = new long[size];
        for (int op = 0; op < 1_000; op++) {
            int index = rng.nextInt(size);
            long delta = rng.nextInt(-50, 100);
            fenwick.add(index, delta);
            naive[index] += delta;
            int count = rng.nextInt(size + 1);
            long expected = 0;
            for (int i = 0; i < count; i++) {
                expected += naive[i];
            }
            assertThat(fenwick.prefixSum(count)).as("op %d count %d", op, count).isEqualTo(expected);
        }
    }

    @Test
    void boundariesShouldBeHonest() {
        FenwickTree fenwick = new FenwickTree(3);
        assertThat(fenwick.size()).isEqualTo(3);
        assertThat(fenwick.prefixSum(0)).isZero();
        assertThat(fenwick.prefixSum(3)).isZero();  // 初值全零
        assertThat(fenwick.total()).isZero();
    }

    @Test
    void outOfBoundsShouldFailFast() {
        FenwickTree fenwick = new FenwickTree(4);
        assertThatThrownBy(() -> fenwick.add(-1, 1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> fenwick.add(4, 1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> fenwick.prefixSum(-1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> fenwick.prefixSum(5)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> fenwick.rangeSum(3, 1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> new FenwickTree(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void largeLongValuesShouldNotOverflow() {
        // 8e18 < Long.MAX（9.22e18）——两路大值全量和精确无损
        FenwickTree fenwick = new FenwickTree(2);
        long big = 4_000_000_000_000_000_000L;
        fenwick.add(0, big);
        fenwick.add(1, big);
        assertThat(fenwick.total()).isEqualTo(2 * big);
    }
}
