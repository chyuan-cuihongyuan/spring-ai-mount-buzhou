package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7003：AvlTree 合同——严格平衡自平衡 BST。随机操作
 * vs TreeMap 圣像（含删）逐步全等；高度界 1.44·log₂(n)；
 * 顺序插入不退化；键序确定性；upsert 覆值；fail-fast。
 */
class AvlTreeTest {

    @Test
    void randomOpsMatchTreeMapOracle() {
        AvlTree tree = new AvlTree();
        TreeMap<Long, Long> oracle = new TreeMap<>();
        Random rng = new Random(7003L);
        for (int op = 0; op < 600; op++) {
            long key = rng.nextInt(200);
            if (rng.nextBoolean()) {
                long value = rng.nextInt(1_000_000);
                tree.put(key, value);
                oracle.put(key, value);
            } else {
                boolean absent = !oracle.containsKey(key);
                if (absent) {
                    long k = key;
                    assertThatThrownBy(() -> tree.remove(k))
                            .isInstanceOf(IllegalArgumentException.class);
                } else {
                    tree.remove(key);
                    oracle.remove(key);
                }
            }
            assertThat(tree.size()).isEqualTo(oracle.size());
            assertThat(tree.get(key))
                    .isEqualTo(oracle.containsKey(key) ? oracle.get(key) : null);
        }
        assertThat(tree.keysInOrder()).containsExactly(oracle.keySet().stream()
                .mapToLong(Long::longValue).toArray());
    }

    @Test
    void heightBoundHoldsUnderAdversarialSequences() {
        AvlTree sequential = new AvlTree();
        for (long key = 0; key < 200; key++) {
            sequential.put(key, key);
        }
        assertThat(sequential.height())
                .as("顺序插入（朴素 BST 最坏退化输入）高度 ≤ 1.44·log₂(202)≈11")
                .isLessThanOrEqualTo(11);
        AvlTree reverse = new AvlTree();
        for (long key = 200; key > 0; key--) {
            reverse.put(key, key);
        }
        assertThat(reverse.height()).isLessThanOrEqualTo(11);
        AvlTree random = new AvlTree();
        Random rng = new Random(7047L);
        for (int i = 0; i < 1000; i++) {
            random.put(rng.nextInt(50_000), i);
        }
        assertThat(random.height()).isLessThanOrEqualTo(15);
    }

    @Test
    void upsertOverwritesWithoutGrowth() {
        AvlTree tree = new AvlTree();
        tree.put(1, 100);
        tree.put(1, 200);
        assertThat(tree.size()).isEqualTo(1);
        assertThat(tree.get(1)).isEqualTo(200);
        assertThat(tree.containsKey(2)).isFalse();
        assertThat(tree.get(2)).isNull();
    }

    @Test
    void emptyTreeAndFailFastContract() {
        AvlTree tree = new AvlTree();
        assertThat(tree.isEmpty()).isTrue();
        assertThat(tree.height()).isZero();
        assertThat(tree.keysInOrder()).isEmpty();
        assertThatThrownBy(() -> tree.remove(5)).isInstanceOf(IllegalArgumentException.class);
    }
}
