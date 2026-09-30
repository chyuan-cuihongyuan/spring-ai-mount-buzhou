package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartesianTreeTest {

    @Test
    void shouldBuildClassicAnchors() {
        // {8, 2, 5, 1, 4}：根 = 最小值 1（下标 3）；左子树 {8,2,5}（根 2）、右子树 {4}
        CartesianTree tree = CartesianTree.of(new int[]{8, 2, 5, 1, 4});
        assertThat(tree.root()).isEqualTo(3);
        assertThat(tree.leftChildOf(3)).isEqualTo(1);
        assertThat(tree.rightChildOf(3)).isEqualTo(4);
        assertThat(tree.parentOf(1)).isEqualTo(3);
        assertThat(tree.parentOf(4)).isEqualTo(3);
        assertThat(tree.leftChildOf(1)).isEqualTo(0);
        assertThat(tree.rightChildOf(1)).isEqualTo(2);
        // 升序数组：右脊链（每点只右孩）
        CartesianTree ascending = CartesianTree.of(new int[]{1, 2, 3});
        assertThat(ascending.root()).isZero();
        assertThat(ascending.leftChildOf(0)).isEqualTo(-1);
        assertThat(ascending.rightChildOf(0)).isEqualTo(1);
        assertThat(ascending.rightChildOf(1)).isEqualTo(2);
    }

    @Test
    void shouldAnswerRmqViaLcaBridge() {
        // RMQ↔LCA 桥：rangeMinIndex(l,r) == 区间最小值首个下标
        int[] values = {8, 2, 5, 1, 4, 3, 9, 1};
        CartesianTree tree = CartesianTree.of(values);
        assertThat(tree.rangeMinIndex(0, 7)).isEqualTo(3);   // 全程最小 1（首见 3）
        assertThat(tree.rangeMinIndex(4, 7)).isEqualTo(7);   // {4,3,9,1} → 7
        assertThat(tree.rangeMinIndex(0, 2)).isEqualTo(1);   // {8,2,5} → 1
        assertThat(tree.rangeMinIndex(2, 2)).isEqualTo(2);   // 单点
        Random random = new Random(17);
        for (int t = 0; t < 60; t++) {
            int n = 1 + random.nextInt(30);
            int[] arr = new int[n];
            for (int i = 0; i < n; i++) {
                arr[i] = random.nextInt(20);
            }
            CartesianTree rt = CartesianTree.of(arr);
            int l = random.nextInt(n);
            int r = l + random.nextInt(n - l);
            int expected = l;
            for (int i = l; i <= r; i++) {
                if (arr[i] < arr[expected]) {
                    expected = i;
                }
            }
            assertThat(rt.rangeMinIndex(l, r)).as("随机 %d 区间 [%d,%d]", t, l, r)
                    .isEqualTo(expected);
        }
    }

    @Test
    void shouldHoldInvariantsAndBeDeterministic() {
        Random random = new Random(71);
        for (int t = 0; t < 30; t++) {
            int n = 1 + random.nextInt(30);
            int[] arr = new int[n];
            for (int i = 0; i < n; i++) {
                arr[i] = random.nextInt(15);
            }
            CartesianTree tree = CartesianTree.of(arr);
            // 堆序不变量：父值 ≤ 子值（同值时父下标 < 子下标）
            for (int v = 0; v < n; v++) {
                int p = tree.parentOf(v);
                if (p != -1) {
                    assertThat(arr[p] <= arr[v]).as("堆序 %d", v).isTrue();
                    if (arr[p] == arr[v]) {
                        assertThat(p).isLessThan(v);
                    }
                }
            }
            // 中序不变量：左<父<右（下标 BST 序）——用左孩下标 < 父 < 右孩下标
            for (int v = 0; v < n; v++) {
                if (tree.leftChildOf(v) != -1) {
                    assertThat(tree.leftChildOf(v)).isLessThan(v);
                }
                if (tree.rightChildOf(v) != -1) {
                    assertThat(tree.rightChildOf(v)).isGreaterThan(v);
                }
            }
        }
        // 确定性双跑
        int[] arr = {3, 1, 4, 1, 5, 9, 2, 6};
        CartesianTree first = CartesianTree.of(arr);
        CartesianTree second = CartesianTree.of(arr);
        for (int v = 0; v < arr.length; v++) {
            assertThat(first.parentOf(v)).isEqualTo(second.parentOf(v));
            assertThat(first.leftChildOf(v)).isEqualTo(second.leftChildOf(v));
        }
    }

    @Test
    void shouldBeFailFast() {
        assertThatThrownBy(() -> CartesianTree.of(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CartesianTree.of(new int[]{}))
                .isInstanceOf(IllegalArgumentException.class);
        CartesianTree tree = CartesianTree.of(new int[]{2, 1, 3});
        assertThatThrownBy(() -> tree.rangeMinIndex(-1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.rangeMinIndex(2, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
