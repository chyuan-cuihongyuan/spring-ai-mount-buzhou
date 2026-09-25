package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6002 / T6206：Treap 合同——BST 键序 + 最小堆优先级
 * 双不变量的期望平衡映射。顺序插入（最坏 BST 输入）高度
 * 有界；扰动混合操作 TreeMap 圣像；同种子同结构/异种子
 * 结构异；fail-fast。
 */
class TreapTest {

    @Test
    void sequentialInsertStaysBoundedAndSorted() {
        Treap treap = new Treap(7L);
        TreeMap<Long, Long> oracle = new TreeMap<>();
        for (long k = 1; k <= 500; k++) {
            treap.put(k, k * 3);
            oracle.put(k, k * 3);
        }
        assertThat(treap.size()).isEqualTo(500);
        assertThat(treap.height()).isLessThanOrEqualTo(32);
        long[] keys = treap.keysInOrder();
        int idx = 0;
        for (Map.Entry<Long, Long> e : oracle.entrySet()) {
            assertThat(keys[idx++]).isEqualTo(e.getKey());
            assertThat(treap.get(e.getKey())).isEqualTo(e.getValue());
        }
    }

    @Test
    void mixedOperationsMatchTreeMapOracle() {
        Treap treap = new Treap(6002L);
        TreeMap<Long, Long> oracle = new TreeMap<>();
        Random rng = new Random(42L);
        for (int i = 0; i < 500; i++) {
            long key = rng.nextLong(300);
            int op = rng.nextInt(3);
            if (op == 0) {
                long value = rng.nextLong();
                treap.put(key, value);
                oracle.put(key, value);
            } else if (op == 1) {
                assertThat(treap.get(key)).isEqualTo(oracle.get(key));
            } else if (oracle.containsKey(key)) {
                treap.remove(key);
                oracle.remove(key);
            }
            if (i % 50 == 0) {
                assertThat(treap.keysInOrder()).containsExactly(
                        oracle.keySet().stream().mapToLong(Long::longValue).toArray());
            }
        }
        assertThat(treap.size()).isEqualTo(oracle.size());
        assertThat(treap.keysInOrder()).containsExactly(
                oracle.keySet().stream().mapToLong(Long::longValue).toArray());
    }

    @Test
    void sameSeedSameStructureDifferentSeedDiffers() {
        long[] keys = new long[120];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = i + 1;
        }
        Treap a = new Treap(11L);
        Treap b = new Treap(11L);
        Treap c = new Treap(999L);
        for (long k : keys) {
            a.put(k, k);
            b.put(k, k);
            c.put(k, k);
        }
        assertThat(b.keysInOrder()).containsExactly(a.keysInOrder());
        assertThat(b.height()).isEqualTo(a.height());
        int differs = 0;
        for (long probe = 1; probe <= keys.length; probe++) {
            a.get(probe);
            c.get(probe);
            if (a.height() != c.height()) {
                differs++;
            }
        }
        assertThat(differs).as("异种子 120 键顺序插入后逐键访问高度应分异").isPositive();
    }

    @Test
    void upsertKeepsSizeAndPriority() {
        Treap treap = new Treap(5L);
        treap.put(10, 1);
        treap.put(20, 2);
        treap.put(30, 3);
        int heightBefore = treap.height();
        treap.put(20, 20);
        assertThat(treap.size()).isEqualTo(3);
        assertThat(treap.height()).isEqualTo(heightBefore);
        assertThat(treap.get(20)).isEqualTo(20);
    }

    @Test
    void failFastContract() {
        Treap treap = new Treap();
        assertThatThrownBy(() -> treap.remove(1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(treap.get(1)).isNull();
        assertThat(treap.height()).isZero();
        treap.put(1, 10);
        assertThatThrownBy(() -> treap.remove(2)).isInstanceOf(IllegalArgumentException.class);
    }
}
