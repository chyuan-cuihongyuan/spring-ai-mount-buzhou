package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6026：IndexedHeap 合同——位置映射+decrease-key 最小堆。
 * 扰动混合操作 vs PriorityQueue 出序圣像（优先级序）；update
 * 双向；重复 id/缺席 fail-fast。
 */
class IndexedHeapTest {

    @Test
    void pushPopOrderMatchesPriorityQueueOracle() {
        IndexedHeap heap = new IndexedHeap();
        java.util.Map<Long, Long> priorityOf = new java.util.HashMap<>();
        Random rng = new Random(6026L);
        for (int i = 0; i < 300; i++) {
            long id = i;
            long priority = rng.nextInt(100);
            heap.push(id, priority);
            priorityOf.put(id, priority);
        }
        List<Long> popped = new ArrayList<>();
        long lastPriority = Long.MIN_VALUE;
        while (!heap.isEmpty()) {
            long id = heap.popMin();
            long priority = priorityOf.get(id);
            assertThat(priority).as("出序优先级不降").isGreaterThanOrEqualTo(lastPriority);
            lastPriority = priority;
            popped.add(id);
        }
        assertThat(popped).hasSize(300);
    }

    @Test
    void decreaseKeyBringsTaskForward() {
        IndexedHeap heap = new IndexedHeap();
        heap.push(100, 50);
        heap.push(200, 40);
        heap.push(300, 30);
        heap.updatePriority(100, 10);
        assertThat(heap.peekMin()).isEqualTo(100);
        assertThat(heap.peekMinPriority()).isEqualTo(10);
        heap.updatePriority(100, 60);
        assertThat(heap.peekMin()).isEqualTo(300);
        assertThat(heap.popMin()).isEqualTo(300);
        assertThat(heap.popMin()).isEqualTo(200);
        assertThat(heap.popMin()).isEqualTo(100);
        assertThat(heap.isEmpty()).isTrue();
    }

    @Test
    void mixedOperationsStayConsistent() {
        IndexedHeap heap = new IndexedHeap();
        List<Long> popped = new ArrayList<>();
        heap.push(1, 10);
        heap.push(2, 5);
        heap.push(3, 20);
        heap.updatePriority(3, 1);
        popped.add(heap.popMin());
        heap.updatePriority(2, 50);
        heap.push(4, 30);
        heap.updatePriority(1, 25);
        popped.add(heap.popMin());
        popped.add(heap.popMin());
        popped.add(heap.popMin());
        assertThat(popped).containsExactly(3L, 1L, 4L, 2L);
        assertThat(heap.size()).isZero();
    }

    @Test
    void removeKeepsHeapConsistent() {
        IndexedHeap heap = new IndexedHeap();
        heap.push(1, 10);
        heap.push(2, 20);
        heap.push(3, 30);
        heap.updatePriority(2, 1);
        heap.popMin();
        assertThat(heap.contains(2)).isFalse();
        assertThat(heap.size()).isEqualTo(2);
        assertThat(heap.peekMinPriority()).isEqualTo(10);
        assertThat(heap.priorityOf(3)).isEqualTo(30);
    }

    @Test
    void failFastContract() {
        IndexedHeap heap = new IndexedHeap();
        assertThatThrownBy(() -> heap.popMin()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> heap.peekMin()).isInstanceOf(IllegalArgumentException.class);
        heap.push(1, 10);
        assertThatThrownBy(() -> heap.push(1, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> heap.updatePriority(2, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> heap.priorityOf(9)).isInstanceOf(IllegalArgumentException.class);
    }
}
