package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeavyLightDecompositionTest {

    /** 经典锚树：0-1, 0-2, 1-3, 1-4, 2-5, 5-6（7 点，根 0）。 */
    private static final int[][] TREE = {{0, 1}, {0, 2}, {1, 3}, {1, 4}, {2, 5}, {5, 6}};

    @Test
    void shouldDecomposeClassicTree() {
        HeavyLightDecomposition hld = HeavyLightDecomposition.of(7, TREE, 0);
        // position 是 0..n-1 双射
        boolean[] seen = new boolean[7];
        for (int v = 0; v < 7; v++) {
            seen[hld.positionOf(v)] = true;
        }
        for (boolean s : seen) {
            assertThat(s).isTrue();
        }
        // 重链：0-1（size[1]=3 > size[2]=3？——并列取先见者）；1 的重儿 3/4 并列取先见
        // 链头不变量：链上任意点 v，head(v) 深度 < depth(v)（链头是链内最浅）
        assertThat(hld.lca(3, 4)).isEqualTo(1);
        assertThat(hld.lca(3, 6)).isEqualTo(0);
        assertThat(hld.lca(6, 5)).isEqualTo(5);
        assertThat(hld.lca(2, 2)).isEqualTo(2);
    }

    @Test
    void shouldCoverPathExactlyWithSegments() {
        HeavyLightDecomposition hld = HeavyLightDecomposition.of(7, TREE, 0);
        int[][] cases = {{3, 4}, {3, 6}, {0, 6}, {2, 6}, {4, 5}};
        for (int c = 0; c < cases.length; c++) {
            List<int[]> segments = hld.pathSegments(cases[c][0], cases[c][1]);
            // 覆盖圣像：段代表的顶点集 == u→v 路径顶点集（位置映射回顶点）
            java.util.Set<Integer> pathVertices = new java.util.TreeSet<>();
            for (int[] seg : segments) {
                for (int pos = seg[0]; pos <= seg[1]; pos++) {
                    pathVertices.add(pos);
                }
            }
            int u = cases[c][0];
            int v = cases[c][1];
            int ancestor = hld.lca(u, v);
            java.util.Set<Integer> expectedVertices = new java.util.TreeSet<>();
            for (int w = u; w != ancestor; w = parentOf(hld, w)) {
                expectedVertices.add(hld.positionOf(w));
            }
            for (int w = v; w != ancestor; w = parentOf(hld, w)) {
                expectedVertices.add(hld.positionOf(w));
            }
            expectedVertices.add(hld.positionOf(ancestor));
            assertThat(pathVertices).as("case %d 路径覆盖", c).isEqualTo(expectedVertices);
            assertThat(segments.size()).as("case %d 段数 O(log)", c).isLessThanOrEqualTo(7);
        }
    }

    private static int parentOf(HeavyLightDecomposition hld, int v) {
        // pathSegments 内 u≠ancestor 时 parent 可达——测试侧用 lca 重导出
        // （用暴力：父 = 邻接中更浅者；此处借 position 序：重扫 TREE）
        for (int[] edge : TREE) {
            if (edge[0] == v) {
                return edge[1];
            }
            if (edge[1] == v) {
                return edge[0];
            }
        }
        throw new IllegalStateException("根无父");
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        HeavyLightDecomposition first = HeavyLightDecomposition.of(7, TREE, 0);
        HeavyLightDecomposition second = HeavyLightDecomposition.of(7, TREE, 0);
        for (int v = 0; v < 7; v++) {
            assertThat(first.positionOf(v)).isEqualTo(second.positionOf(v));
            assertThat(first.headOf(v)).isEqualTo(second.headOf(v));
        }
        assertThatThrownBy(() -> HeavyLightDecomposition.of(0, new int[][]{}, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HeavyLightDecomposition.of(2, new int[][]{}, 0))
                .hasMessageContaining("n−1");
        assertThatThrownBy(() -> HeavyLightDecomposition.of(2, new int[][]{{0, 0}}, 0))
                .hasMessageContaining("自环");
        assertThatThrownBy(() -> HeavyLightDecomposition.of(3, new int[][]{{0, 1}, {1, 2}}, 3))
                .hasMessageContaining("根越域");
        assertThatThrownBy(() -> HeavyLightDecomposition.of(4,
                new int[][]{{0, 1}, {1, 2}, {2, 0}}, 0))
                .hasMessageContaining("非连通或带环");
    }

    @Test
    void shouldAgreeWithLcaLiftingOnRandomTrees() {
        // 互证圣像：随机树 HLD.lca == LcaLifting.lca
        Random random = new Random(97);
        for (int t = 0; t < 40; t++) {
            int n = 1 + random.nextInt(30);
            int[][] es = new int[Math.max(0, n - 1)][];
            for (int v = 1; v < n; v++) {
                es[v - 1] = new int[]{random.nextInt(v), v};
            }
            if (n == 1) {
                assertThat(HeavyLightDecomposition.of(1, new int[][]{}, 0).lca(0, 0)).isZero();
                continue;
            }
            HeavyLightDecomposition hld = HeavyLightDecomposition.of(n, es, 0);
            LcaLifting lifting = new LcaLifting(n, 0);
            for (int[] e : es) {
                lifting.addEdge(e[0], e[1]);
            }
            lifting.build();
            for (int q = 0; q < 20; q++) {
                int u = random.nextInt(n);
                int v = random.nextInt(n);
                assertThat(hld.lca(u, v)).as("树 %d 查询 %d", t, q)
                        .isEqualTo(lifting.lca(u, v));
            }
            // 路径段覆盖圣像：段内位置数合计 == 路径顶点数
            int u = random.nextInt(n);
            int v = random.nextInt(n);
            int pathLength = 0;
            int ancestor = hld.lca(u, v);
            for (int w = u; w != ancestor;) {
                pathLength++;
                w = parentIn(w, es);
            }
            for (int w = v; w != ancestor;) {
                pathLength++;
                w = parentIn(w, es);
            }
            pathLength++;
            int covered = 0;
            for (int[] seg : hld.pathSegments(u, v)) {
                covered += seg[1] - seg[0] + 1;
            }
            assertThat(covered).as("树 %d 路径覆盖数", t).isEqualTo(pathLength);
        }
    }

    private static int parentIn(int v, int[][] edges) {
        for (int[] e : edges) {
            if (e[1] == v) {
                return e[0];
            }
        }
        throw new IllegalStateException("根无父");
    }
}
