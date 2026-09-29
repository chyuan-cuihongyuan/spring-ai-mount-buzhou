package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class IntervalHeapTest {

    @Test
    void shouldDrainInOrderFromBothEnds() {
        IntervalHeap heap = new IntervalHeap();
        long[] values = {5, 3, 8, 1, 9, 2, 7, 4, 6, 5};
        for (long value : values) {
            heap.offer(value);
        }
        assertThat(heap.size()).isEqualTo(10);
        assertThat(heap.peekMin()).isEqualTo(1L);
        assertThat(heap.peekMax()).isEqualTo(9L);
        long[] mins = {heap.pollMin(), heap.pollMin(), heap.pollMin()};
        assertThat(mins).containsExactly(1L, 2L, 3L);
        long[] maxes = {heap.pollMax(), heap.pollMax(), heap.pollMax()};
        assertThat(maxes).containsExactly(9L, 8L, 7L);
        assertThat(heap.pollMin()).isEqualTo(4L);
        assertThat(heap.pollMax()).isEqualTo(6L);
        assertThat(heap.pollMin()).isEqualTo(5L);
        assertThat(heap.pollMax()).isEqualTo(5L);
        assertThat(heap.pollMin()).isNull();
        assertThat(heap.pollMax()).isNull();
        assertThat(heap.size()).isEqualTo(0);
    }

    @Test
    void shouldMatchTreeMapOracleOnRandomOperations() {
        Random random = new Random(8018);
        IntervalHeap heap = new IntervalHeap();
        TreeMap<Long, Integer> oracle = new TreeMap<>();
        for (int round = 0; round < 1000; round++) {
            int op = random.nextInt(10);
            long value = random.nextInt(100);
            if (op < 5) {
                heap.offer(value);
                oracle.merge(value, 1, Integer::sum);
            } else if (op < 7) {
                Long min = heap.pollMin();
                Long expected = oracle.isEmpty() ? null : oracle.firstKey();
                if (expected != null && oracle.merge(expected, -1, Integer::sum) == 0) {
                    oracle.remove(expected);
                }
                assertThat(min).isEqualTo(expected);
            } else if (op < 9) {
                Long max = heap.pollMax();
                Long expected = oracle.isEmpty() ? null : oracle.lastKey();
                if (expected != null && oracle.merge(expected, -1, Integer::sum) == 0) {
                    oracle.remove(expected);
                }
                assertThat(max).isEqualTo(expected);
            } else {
                assertThat(heap.peekMin()).isEqualTo(oracle.isEmpty() ? null : oracle.firstKey());
                assertThat(heap.peekMax()).isEqualTo(oracle.isEmpty() ? null : oracle.lastKey());
            }
            assertThat(heap.size()).isEqualTo(oracle.values().stream().mapToInt(Integer::intValue).sum());
        }
    }

    @Test
    void shouldHandleEmptyAndSingleElement() {
        IntervalHeap heap = new IntervalHeap();
        assertThat(heap.pollMin()).isNull();
        assertThat(heap.pollMax()).isNull();
        assertThat(heap.peekMin()).isNull();
        heap.offer(42);
        assertThat(heap.peekMin()).isEqualTo(42L);
        assertThat(heap.peekMax()).isEqualTo(42L);
        assertThat(heap.pollMax()).isEqualTo(42L);
        assertThat(heap.size()).isEqualTo(0);
        heap.offer(1);
        heap.offer(2);
        assertThat(heap.pollMax()).isEqualTo(2L);
        assertThat(heap.pollMin()).isEqualTo(1L);
    }
}
