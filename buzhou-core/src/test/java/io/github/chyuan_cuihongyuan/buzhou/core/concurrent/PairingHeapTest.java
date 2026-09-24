package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5048 / T6198：配对堆合同——插入/弹出有序、TreeMap
 * 圣像、meld O(1) 挂钩与所有权清空、两趟合并确定性、
 * fail-fast。
 */
class PairingHeapTest {

    private static final int ORACLE_COUNT = 300;

    private static final int STRIDE = 37;

    private static final int MODULUS = 97;

    @Test
    void insertsShouldExtractInSortedOrder() {
        PairingHeap heap = new PairingHeap();
        long[] keys = {5, 3, 8, 1, 9, 2, 7};
        for (long key : keys) {
            heap.insert(key);
        }
        assertThat(heap.size()).isEqualTo(keys.length);
        assertThat(heap.peekMin()).isEqualTo(1);
        for (long expected : new long[]{1, 2, 3, 5, 7, 8, 9}) {
            assertThat(heap.extractMin()).isEqualTo(expected);
        }
        assertThat(heap.isEmpty()).isTrue();
    }

    @Test
    void scatteredInsertsShouldMatchTreeMapOracle() {
        PairingHeap heap = new PairingHeap();
        TreeMap<Long, Integer> oracle = new TreeMap<>();
        for (int i = 0; i < ORACLE_COUNT; i++) {
            long key = (i * STRIDE) % MODULUS;
            heap.insert(key);
            oracle.merge(key, 1, Integer::sum);
        }
        assertThat(heap.size()).isEqualTo(ORACLE_COUNT);
        List<Long> extracted = new ArrayList<>();
        while (!heap.isEmpty()) {
            extracted.add(heap.extractMin());
        }
        List<Long> expected = new ArrayList<>();
        for (java.util.Map.Entry<Long, Integer> entry : oracle.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                expected.add(entry.getKey());
            }
        }
        assertThat(extracted).containsExactlyElementsOf(expected);
    }

    @Test
    void meldShouldHookRootsAndEmptyTheOther() {
        PairingHeap first = new PairingHeap();
        first.insert(3);
        first.insert(10);
        PairingHeap second = new PairingHeap();
        second.insert(1);
        second.insert(8);
        first.meld(second);
        assertThat(first.size()).isEqualTo(4);
        assertThat(second.size()).isZero();
        assertThat(second.isEmpty()).isTrue();
        List<Long> extracted = new ArrayList<>();
        while (!first.isEmpty()) {
            extracted.add(first.extractMin());
        }
        assertThat(extracted).containsExactly(1L, 3L, 8L, 10L);
    }

    @Test
    void sameOperationsShouldProduceSameExtractionSequence() {
        PairingHeap first = new PairingHeap();
        PairingHeap second = new PairingHeap();
        for (int round = 0; round < 2; round++) {
            PairingHeap heap = round == 0 ? first : second;
            for (int i = 0; i < 20; i++) {
                heap.insert((i * 11) % 29);
            }
            heap.extractMin();
            heap.extractMin();
            heap.insert(-1);
        }
        List<Long> fromFirst = new ArrayList<>();
        List<Long> fromSecond = new ArrayList<>();
        while (!first.isEmpty()) {
            fromFirst.add(first.extractMin());
        }
        while (!second.isEmpty()) {
            fromSecond.add(second.extractMin());
        }
        assertThat(fromFirst).containsExactlyElementsOf(fromSecond);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        PairingHeap heap = new PairingHeap();
        assertThatThrownBy(heap::peekMin).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(heap::extractMin).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> heap.meld(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> heap.meld(heap)).isInstanceOf(IllegalArgumentException.class);
    }
}
