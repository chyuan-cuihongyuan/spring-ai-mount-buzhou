package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6001 / T6204：SplayTree 合同——访问即伸展的自调整
 * 有序映射。顺序插入（普通 BST 最坏输入）中序全等；扰动混合
 * 操作 TreeMap 圣像；访问后根=被访键（结构确定性）；删除后
 * 根=前驱；双实例同操作同根；fail-fast。
 */
class SplayTreeTest {

    @Test
    void sequentialInsertInOrderStaysSorted() {
        SplayTree tree = new SplayTree();
        for (long k = 1; k <= 500; k++) {
            tree.put(k, k * 2);
        }
        assertThat(tree.size()).isEqualTo(500);
        long[] keys = tree.keysInOrder();
        for (int i = 0; i < 500; i++) {
            assertThat(keys[i]).isEqualTo(i + 1);
            assertThat(tree.get(i + 1)).isEqualTo((i + 1) * 2);
        }
    }

    @Test
    void mixedOperationsMatchTreeMapOracle() {
        SplayTree tree = new SplayTree();
        TreeMap<Long, Long> oracle = new TreeMap<>();
        Random rng = new Random();
        rng.setSeed(6001L);
        for (int i = 0; i < 1000; i++) {
            long key = rng.nextLong(200);
            int op = rng.nextInt(3);
            if (op == 0) {
                long value = rng.nextLong();
                tree.put(key, value);
                oracle.put(key, value);
            } else if (op == 1) {
                assertThat(tree.get(key)).isEqualTo(oracle.get(key));
            } else if (oracle.containsKey(key)) {
                tree.remove(key);
                oracle.remove(key);
            }
            if (i % 50 == 0) {
                assertThat(tree.keysInOrder()).containsExactly(
                        oracle.keySet().stream().mapToLong(Long::longValue).toArray());
            }
        }
        assertThat(tree.size()).isEqualTo(oracle.size());
        assertThat(tree.keysInOrder()).containsExactly(
                oracle.keySet().stream().mapToLong(Long::longValue).toArray());
    }

    @Test
    void accessSplaysTargetToRoot() {
        SplayTree tree = new SplayTree();
        for (long k = 1; k <= 7; k++) {
            tree.put(k, k);
        }
        assertThat(tree.get(4)).isEqualTo(4);
        assertThat(tree.rootKey()).isEqualTo(4);
        assertThat(tree.get(2)).isEqualTo(2);
        assertThat(tree.rootKey()).isEqualTo(2);
        tree.put(4, 40);
        assertThat(tree.get(4)).isEqualTo(40);
        assertThat(tree.rootKey()).isEqualTo(4);
        assertThat(tree.size()).isEqualTo(7);
    }

    @Test
    void removeReplacesRootWithPredecessor() {
        SplayTree tree = new SplayTree();
        for (long k = 1; k <= 7; k++) {
            tree.put(k, k);
        }
        tree.remove(7);
        assertThat(tree.rootKey()).isEqualTo(6);
        assertThat(tree.keysInOrder()).containsExactly(1L, 2, 3, 4, 5, 6);
        tree.remove(1);
        assertThat(tree.rootKey()).isEqualTo(5);
        assertThat(tree.keysInOrder()).containsExactly(2L, 3, 4, 5, 6);
        assertThat(tree.size()).isEqualTo(5);
    }

    @Test
    void twoInstancesSameOpsSameStructure() {
        List<long[]> ops = new ArrayList<>();
        Random rng = new Random();
        rng.setSeed(4242L);
        for (int i = 0; i < 400; i++) {
            ops.add(new long[]{rng.nextInt(4), rng.nextLong(100), rng.nextLong()});
        }
        SplayTree a = new SplayTree();
        SplayTree b = new SplayTree();
        for (long[] op : ops) {
            long kind = op[0];
            long key = op[1];
            long value = op[2];
            if (kind == 0) {
                a.put(key, value);
                b.put(key, value);
            } else if (kind == 1) {
                a.get(key);
                b.get(key);
            } else if (kind == 2) {
                boolean present = a.containsKey(key);
                assertThat(b.containsKey(key)).as("step %d 在位不一致", key).isEqualTo(present);
                if (present) {
                    a.remove(key);
                    b.remove(key);
                }
            }
            if (a.size() > 0) {
                assertThat(b.rootKey()).as("step %d 根不一致", op[1]).isEqualTo(a.rootKey());
            }
        }
        assertThat(b.keysInOrder()).containsExactly(a.keysInOrder());
    }

    @Test
    void failFastContract() {
        SplayTree tree = new SplayTree();
        assertThatThrownBy(() -> tree.rootKey()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.remove(1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(tree.get(1)).isNull();
        tree.put(1, 10);
        assertThatThrownBy(() -> tree.remove(2)).isInstanceOf(IllegalArgumentException.class);
        assertThat(tree.containsKey(2)).isFalse();
    }
}
