package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeftistHeapTest {

    @Test
    void shouldMergeAndDrainInOrder() {
        LeftistHeap first = new LeftistHeap();
        LeftistHeap second = new LeftistHeap();
        first.offer(5);
        first.offer(1);
        first.offer(9);
        second.offer(3);
        second.offer(7);
        second.offer(2);
        first.merge(second);
        assertThat(second.size()).isEqualTo(0);
        assertThat(first.size()).isEqualTo(6);
        assertThat(first.leftistInvariantHolds()).isTrue();
        long[] drained = new long[6];
        for (int i = 0; i < 6; i++) {
            drained[i] = first.poll();
        }
        assertThat(drained).containsExactly(1L, 2L, 3L, 5L, 7L, 9L);
        assertThat(first.poll()).isNull();
        assertThat(first.peek()).isNull();
    }

    @Test
    void shouldDrainSortedAfterBulkOffers() {
        Random random = new Random(8019);
        for (int round = 0; round < 50; round++) {
            LeftistHeap heap = new LeftistHeap();
            ArrayList<Long> expected = new ArrayList<>();
            for (int i = 0; i < 300; i++) {
                long value = random.nextInt(1000);
                heap.offer(value);
                expected.add(value);
            }
            assertThat(heap.leftistInvariantHolds()).isTrue();
            Collections.sort(expected);
            long[] drained = new long[expected.size()];
            for (int i = 0; i < drained.length; i++) {
                drained[i] = heap.poll();
            }
            long[] expectedArray = new long[expected.size()];
            for (int i = 0; i < expectedArray.length; i++) {
                expectedArray[i] = expected.get(i);
            }
            assertThat(drained).isEqualTo(expectedArray);
        }
    }

    @Test
    void shouldMatchPriorityQueueOracleOnSingleHeap() {
        Random random = new Random(8020);
        LeftistHeap heap = new LeftistHeap();
        PriorityQueue<Long> oracle = new PriorityQueue<>();
        for (int round = 0; round < 2000; round++) {
            int op = random.nextInt(10);
            long value = random.nextInt(500);
            if (op < 6) {
                heap.offer(value);
                oracle.add(value);
            } else if (op < 9) {
                Long polled = heap.poll();
                Long expected = oracle.poll();
                assertThat(polled).as("round=%d", round).isEqualTo(expected);
            } else {
                assertThat(heap.peek()).isEqualTo(oracle.isEmpty() ? null : oracle.peek());
            }
            assertThat(heap.size()).isEqualTo(oracle.size());
            if (round % 200 == 0) {
                assertThat(heap.leftistInvariantHolds()).isTrue();
            }
        }
    }

    @Test
    void shouldMergeChainAndDrainSorted() {
        Random random = new Random(8021);
        ArrayList<Long> expected = new ArrayList<>();
        LeftistHeap merged = new LeftistHeap();
        for (int shard = 0; shard < 8; shard++) {
            LeftistHeap shardHeap = new LeftistHeap();
            for (int i = 0; i < 60; i++) {
                long value = random.nextInt(2000);
                shardHeap.offer(value);
                expected.add(value);
            }
            merged.merge(shardHeap);
            assertThat(shardHeap.size()).isEqualTo(0);
            assertThat(merged.leftistInvariantHolds()).isTrue();
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
    }

    @Test
    void shouldFailFastOnSelfMergeAndRespectEmpty() {
        LeftistHeap heap = new LeftistHeap();
        assertThatThrownBy(() -> heap.merge(heap))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(heap.poll()).isNull();
        assertThat(heap.peek()).isNull();
        heap.offer(7);
        assertThat(heap.leftistInvariantHolds()).isTrue();
        assertThat(heap.poll()).isEqualTo(7L);
        assertThat(heap.leftistInvariantHolds()).isTrue();
    }
}
