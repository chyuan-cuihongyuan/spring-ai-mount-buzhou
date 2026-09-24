package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5032 / T6166：B+ 树合同——顺序/扰动插入全序正确、
 * upsert 覆盖不增位、树高有界、叶链有序、TreeMap 圣像、
 * fail-fast。
 */
class BPlusTreeTest {

    private static final int SMALL_ORDER = 2;

    private static final int ORDER_FOUR = 4;

    private static final int SEQUENTIAL_KEYS = 64;

    private static final int ORACLE_KEYS = 500;

    private static final int STRIDE = 37;

    private static final int STRIDE_MODULUS = 97;

    private static final int MAX_HEIGHT_FOR_ORDER4 = 5;

    @Test
    void sequentialInsertsShouldStayOrderedAndQueryable() {
        BPlusTree tree = new BPlusTree(ORDER_FOUR);
        for (int i = 1; i <= SEQUENTIAL_KEYS; i++) {
            tree.put(i, i * 10);
        }
        assertThat(tree.size()).isEqualTo(SEQUENTIAL_KEYS);
        for (int i = 1; i <= SEQUENTIAL_KEYS; i++) {
            assertThat(tree.getOrDefault(i, -1)).isEqualTo(i * 10);
            assertThat(tree.containsKey(i)).isTrue();
        }
        assertThat(tree.containsKey(SEQUENTIAL_KEYS + 1)).isFalse();
        List<BPlusTree.Entry> entries = tree.entriesInOrder();
        assertThat(entries).hasSize(SEQUENTIAL_KEYS);
        for (int i = 0; i < SEQUENTIAL_KEYS; i++) {
            assertThat(entries.get(i).key()).isEqualTo(i + 1);
        }
    }

    @Test
    void upsertShouldOverwriteWithoutGrowing() {
        BPlusTree tree = new BPlusTree(SMALL_ORDER);
        tree.put(7, 1);
        tree.put(7, 2);
        assertThat(tree.size()).isEqualTo(1);
        assertThat(tree.getOrDefault(7, -1)).isEqualTo(2);
    }

    @Test
    void scatteredInsertsShouldMatchTreeMapOracle() {
        BPlusTree tree = new BPlusTree(ORDER_FOUR);
        TreeMap<Integer, Integer> oracle = new TreeMap<>();
        for (int i = 0; i < ORACLE_KEYS; i++) {
            int key = (i * STRIDE) % STRIDE_MODULUS;
            tree.put(key, i);
            oracle.put(key, i);
        }
        assertThat(tree.size()).isEqualTo(oracle.size());
        List<BPlusTree.Entry> entries = tree.entriesInOrder();
        assertThat(entries).hasSize(oracle.size());
        int idx = 0;
        for (Map.Entry<Integer, Integer> expected : oracle.entrySet()) {
            assertThat(entries.get(idx).key()).isEqualTo(expected.getKey());
            assertThat(entries.get(idx).value()).isEqualTo(expected.getValue());
            idx++;
        }
        oracle.forEach((k, v) -> assertThat(tree.getOrDefault(k, -1)).isEqualTo(v));
    }

    @Test
    void heightShouldStayLogarithmicUnderLoad() {
        BPlusTree single = new BPlusTree(ORDER_FOUR);
        single.put(1, 1);
        assertThat(single.height()).isEqualTo(1);
        BPlusTree loaded = new BPlusTree(ORDER_FOUR);
        for (int i = 0; i < SEQUENTIAL_KEYS; i++) {
            loaded.put(i, i);
        }
        assertThat(loaded.height()).isGreaterThanOrEqualTo(2);
        assertThat(loaded.height()).isLessThanOrEqualTo(MAX_HEIGHT_FOR_ORDER4);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new BPlusTree(1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BPlusTree(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
