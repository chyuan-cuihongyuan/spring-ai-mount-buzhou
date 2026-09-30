package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CentroidDecompositionTest {

    @Test
    void shouldDecomposeClassicTree() {
        // 星形：中心 0 挂 1..4——重心必是 0（删后最大子块 1）
        int[][] star = {{0, 1}, {0, 2}, {0, 3}, {0, 4}};
        CentroidDecomposition starCd = CentroidDecomposition.of(5, star);
        assertThat(starCd.rootCentroid()).isZero();
        assertThat(starCd.componentSizeOf(0)).isEqualTo(5);
        assertThat(starCd.childrenInCentroidTree(0)).containsExactly(1, 2, 3, 4);
        // 链 0-1-2-3-4：重心必是 2（删后两半各 2）
        int[][] chain = {{0, 1}, {1, 2}, {2, 3}, {3, 4}};
        CentroidDecomposition chainCd = CentroidDecomposition.of(5, chain);
        assertThat(chainCd.rootCentroid()).isEqualTo(2);
        assertThat(chainCd.componentSizeOf(2)).isEqualTo(5);
        assertThat(chainCd.childrenInCentroidTree(2)).hasSize(2);
        // 单点
        CentroidDecomposition single = CentroidDecomposition.of(1, new int[][]{});
        assertThat(single.rootCentroid()).isZero();
        assertThat(single.parentInCentroidTree(0)).isEqualTo(-1);
    }

    @Test
    void shouldHoldCentroidTreeInvariants() {
        // 经典锚树：0-1, 0-2, 1-3, 1-4, 2-5, 5-6
        int[][] tree = {{0, 1}, {0, 2}, {1, 3}, {1, 4}, {2, 5}, {5, 6}};
        CentroidDecomposition cd = CentroidDecomposition.of(7, tree);
        // 不变量 1：父块大小 > 子块大小（严格递减——树高 O(log n)）
        for (int v = 0; v < 7; v++) {
            int p = cd.parentInCentroidTree(v);
            if (p != -1) {
                assertThat(cd.componentSizeOf(p)).isGreaterThan(cd.componentSizeOf(v));
            }
        }
        // 不变量 2：每个顶点恰一次作为重心（父指针构成以根为根的树）
        int roots = 0;
        for (int v = 0; v < 7; v++) {
            if (cd.parentInCentroidTree(v) == -1) {
                roots++;
            }
        }
        assertThat(roots).isEqualTo(1);
        assertThat(cd.rootCentroid()).isZero(); // 子块 {1,3,4}/{2,5,6} 各 3——0 为均衡重心
        // 不变量 3：重心树高 ≤ log2(n) 上界的松弛（7 点 ≤ 3 层深）
        int maxDepth = 0;
        for (int v = 0; v < 7; v++) {
            int depth = 0;
            for (int u = v; u != -1; u = cd.parentInCentroidTree(u)) {
                depth++;
            }
            maxDepth = Math.max(maxDepth, depth);
        }
        assertThat(maxDepth).isLessThanOrEqualTo(3);
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        int[][] tree = {{0, 1}, {0, 2}, {1, 3}, {1, 4}, {2, 5}, {5, 6}};
        CentroidDecomposition first = CentroidDecomposition.of(7, tree);
        CentroidDecomposition second = CentroidDecomposition.of(7, tree);
        for (int v = 0; v < 7; v++) {
            assertThat(first.parentInCentroidTree(v)).isEqualTo(second.parentInCentroidTree(v));
            assertThat(first.componentSizeOf(v)).isEqualTo(second.componentSizeOf(v));
        }
        assertThatThrownBy(() -> CentroidDecomposition.of(0, new int[][]{}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CentroidDecomposition.of(2, new int[][]{{0, 0}}))
                .hasMessageContaining("自环");
        assertThatThrownBy(() -> CentroidDecomposition.of(2, new int[][]{{0, 0}, {0, 1}}))
                .hasMessageContaining("树边数");
        assertThatThrownBy(() -> CentroidDecomposition.of(4,
                new int[][]{{0, 1}, {1, 2}, {2, 0}}))
                .hasMessageContaining("非连通或带环");
        assertThatThrownBy(() -> CentroidDecomposition.of(3, new int[][]{{0, 1}}))
                .hasMessageContaining("n−1");
    }

    @Test
    void shouldHoldInvariantsOnRandomTrees() {
        Random random = new Random(63);
        for (int t = 0; t < 40; t++) {
            int n = 1 + random.nextInt(40);
            int[][] es = new int[Math.max(0, n - 1)][];
            for (int v = 1; v < n; v++) {
                es[v - 1] = new int[]{random.nextInt(v), v};
            }
            CentroidDecomposition cd = CentroidDecomposition.of(n, es);
            int roots = 0;
            int maxDepth = 0;
            for (int v = 0; v < n; v++) {
                int depth = 0;
                int p = cd.parentInCentroidTree(v);
                if (p == -1) {
                    roots++;
                } else {
                    assertThat(cd.componentSizeOf(p)).as("树 %d 点 %d 父块更大", t, v)
                            .isGreaterThan(cd.componentSizeOf(v));
                }
                for (int u = v; u != -1; u = cd.parentInCentroidTree(u)) {
                    depth++;
                }
                maxDepth = Math.max(maxDepth, depth);
            }
            assertThat(roots).isEqualTo(1);
            // 树高 O(log n)：≤ 2*log2(n)+2 松弛上界
            int bound = n <= 1 ? 1 : (int) Math.ceil(Math.log(n) / Math.log(2)) * 2 + 1;
            assertThat(maxDepth).as("树 %d 重心树高", t).isLessThanOrEqualTo(bound);
        }
    }
}
