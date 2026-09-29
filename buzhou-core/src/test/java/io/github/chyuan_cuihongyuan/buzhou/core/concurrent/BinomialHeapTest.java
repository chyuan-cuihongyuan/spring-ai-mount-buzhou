package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BinomialHeapTest {

    @Test
    void shouldMergeForestAndDrainInOrder() {
        BinomialHeap first = new BinomialHeap();
        BinomialHeap second = new BinomialHeap();
        first.offer(5);
        first.offer(1);
        first.offer(9);
        second.offer(3);
        second.offer(7);
        second.offer(2);
        assertThat(first.forestInvariantHolds()).isTrue();
        first.merge(second);
        assertThat(second.size()).isEqualTo(0);
        assertThat(first.size()).isEqualTo(6);
        assertThat(first.forestInvariantHolds()).isTrue();
        long[] drained = new long[6];
        for (int i = 0; i < 6; i++) {
            drained[i] = first.poll();
        }
        assertThat(drained).containsExactly(1L, 2L, 3L, 5L, 7L, 9L);
        assertThat(first.poll()).isNull();
        assertThat(first.forestInvariantHolds()).isTrue();
    }

    @Test
    void shouldMatchPriorityQueueOracleOnRandomOperations() {
        Random random = new Random(8020);
        BinomialHeap heap = new BinomialHeap();
        BinomialHeap spare = new BinomialHeap();
        PriorityQueue<Long> oracle = new PriorityQueue<>();
        for (int round = 0; round < 500; round++) {
            int op = random.nextInt(10);
            long value = random.nextInt(500);
            if (op < 5) {
                heap.offer(value);
                oracle.add(value);
            } else if (op < 7) {
                spare.offer(value);
                oracle.add(value);
            } else if (op < 9) {
                if (spare.size() > 0) {
                    heap.merge(spare);
                }
                Long polled = heap.poll();
                Long expected = oracle.poll();
                assertThat(polled).as("round=%d", round).isEqualTo(expected);
            } else {
                if (spare.size() > 0) {
                    heap.merge(spare);
                }
                assertThat(heap.peek()).isEqualTo(oracle.isEmpty() ? null : oracle.peek());
            }
            assertThat(heap.size() + spare.size()).isEqualTo(oracle.size());
            if (round % 50 == 0) {
                assertThat(heap.forestInvariantHolds()).isTrue();
            }
        }
        assertThat(heap.forestInvariantHolds()).isTrue();
    }

    @Test
    void shouldMergeChainAndDrainSorted() {
        Random random = new Random(8021);
        ArrayList<Long> expected = new ArrayList<>();
        BinomialHeap merged = new BinomialHeap();
        for (int shard = 0; shard < 8; shard++) {
            BinomialHeap shardHeap = new BinomialHeap();
            for (int i = 0; i < 60; i++) {
                long value = random.nextInt(2000);
                shardHeap.offer(value);
                expected.add(value);
            }
            merged.merge(shardHeap);
            assertThat(shardHeap.size()).isEqualTo(0);
            assertThat(merged.forestInvariantHolds()).isTrue();
        }
        assertThat(merged.size()).isEqualTo(480);
        Collections.sort(expected);
        long[] drained = new long[expected.size()];
        for (int i = 0; i < drained.length; i++) {
            drained[i] = merged.poll();
        }
        long[] expectedArray = new long[expected.size()];
        for (int i = 0; i < expectedArray.length; i++) {
            expectedArray[i] = expected.get(i);
        }
        assertThat(drained).isEqualTo(expectedArray);
        assertThatThrownBy(() -> merged.merge(merged))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
