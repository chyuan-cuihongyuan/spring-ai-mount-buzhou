package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class ScapegoatTreeTest {

    @Test
    void shouldStayBalancedUnderSequentialInserts() {
        ScapegoatTree tree = new ScapegoatTree();
        for (int k = 0; k < 500; k++) {
            assertThat(tree.insert(k)).isTrue();
        }
        assertThat(tree.size()).isEqualTo(500);
        // 顺序插入 500 键：替罪羊 α=0.75 保证树高 ≤ log_{1/α}(n)+2（≈24；朴素 BST 此处高 500）
        int alphaBound = (int) Math.ceil(Math.log(500) / Math.log(4.0 / 3.0)) + 2;
        assertThat(tree.height()).isLessThanOrEqualTo(alphaBound);
        int[] inOrder = tree.inOrder();
        assertThat(inOrder).hasSize(500);
        assertThat(inOrder).isSorted();
        assertThat(inOrder[0]).isZero();
        assertThat(inOrder[499]).isEqualTo(499);
        // 重复插入幂等
        assertThat(tree.insert(250)).isFalse();
        assertThat(tree.size()).isEqualTo(500);
    }

    @Test
    void shouldMirrorTreeSetOnRandomOps() {
        // 圣像：随机插入/删除/查询 vs TreeSet 全等
        Random random = new Random(101);
        ScapegoatTree tree = new ScapegoatTree();
        TreeSet<Integer> mirror = new TreeSet<>();
        for (int op = 0; op < 4000; op++) {
            int key = random.nextInt(300);
            int dice = random.nextInt(10);
            if (dice < 5) {
                assertThat(tree.insert(key)).isEqualTo(mirror.add(key));
            } else if (dice < 8) {
                assertThat(tree.remove(key)).isEqualTo(mirror.remove(key));
            } else {
                assertThat(tree.contains(key)).isEqualTo(mirror.contains(key));
            }
            assertThat(tree.size()).isEqualTo(mirror.size());
        }
        int[] inOrder = tree.inOrder();
        int[] expected = mirror.stream().mapToInt(Integer::intValue).toArray();
        assertThat(inOrder).containsExactly(expected);
        // 删除后仍平衡
        assertThat(tree.height())
                .isLessThanOrEqualTo((int) (2 * Math.ceil(Math.log(tree.size() + 2) / Math.log(2))) + 2);
    }

    @Test
    void shouldBeDeterministicAcrossRuns() {
        int[] keys = {50, 30, 70, 20, 40, 60, 80, 10, 45, 75};
        ScapegoatTree first = new ScapegoatTree();
        ScapegoatTree second = new ScapegoatTree();
        for (int k : keys) {
            first.insert(k);
            second.insert(k);
        }
        assertThat(first.inOrder()).isEqualTo(second.inOrder());
        assertThat(first.height()).isEqualTo(second.height());
        // 逐个删除到空
        for (int k : keys) {
            assertThat(first.remove(k)).isTrue();
        }
        assertThat(first.size()).isZero();
        assertThat(first.inOrder()).isEmpty();
        assertThat(first.remove(42)).isFalse();
        assertThat(first.contains(42)).isFalse();
    }
}
