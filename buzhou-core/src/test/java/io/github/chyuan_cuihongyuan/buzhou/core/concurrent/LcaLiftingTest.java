package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7010：LcaLifting 合同——倍增祖先跳 LCA。手锚二叉树；
 * 随机树 vs 父链爬升暴力圣像；kthAncestor/深度/距离一致性；
 * build 前后边界；fail-fast。
 */
class LcaLiftingTest {

    @Test
    void handAnchoredBinaryTree() {
        LcaLifting tree = new LcaLifting(7, 0);
        int[][] edges = {{0, 1}, {0, 2}, {1, 3}, {1, 4}, {2, 5}, {2, 6}};
        for (int[] e : edges) {
            tree.addEdge(e[0], e[1]);
        }
        tree.build();
        assertThat(tree.lca(3, 4)).isEqualTo(1);
        assertThat(tree.lca(3, 5)).isEqualTo(0);
        assertThat(tree.lca(5, 6)).isEqualTo(2);
        assertThat(tree.lca(0, 6)).isEqualTo(0);
        assertThat(tree.depth(3)).isEqualTo(2);
        assertThat(tree.distance(3, 4)).isEqualTo(2);
        assertThat(tree.distance(3, 6)).isEqualTo(4);
        assertThat(tree.kthAncestor(3, 2)).isZero();
        assertThat(tree.kthAncestor(3, 0)).isEqualTo(3);
    }

    @Test
    void randomTreeMatchesParentWalkOracle() {
        Random rng = new Random(7010L);
        for (int round = 0; round < 100; round++) {
            int n = 2 + rng.nextInt(60);
            int[] parent = new int[n];
            parent[0] = 0;
            for (int v = 1; v < n; v++) {
                parent[v] = rng.nextInt(v);
            }
            LcaLifting tree = new LcaLifting(n, 0);
            for (int v = 1; v < n; v++) {
                tree.addEdge(parent[v], v);
            }
            tree.build();
            for (int q = 0; q < 30; q++) {
                int u = rng.nextInt(n);
                int v = rng.nextInt(n);
                int oracle = walkLca(parent, u, v);
                assertThat(tree.lca(u, v))
                        .as("round %d lca(%d,%d)", round, u, v).isEqualTo(oracle);
                assertThat(tree.depth(u)).isEqualTo(walkDepth(parent, u));
                int k = rng.nextInt(walkDepth(parent, u) + 1);
                int ancestor = u;
                for (int step = 0; step < k; step++) {
                    ancestor = parent[ancestor];
                }
                assertThat(tree.kthAncestor(u, k)).isEqualTo(ancestor);
            }
        }
    }

    @Test
    void buildLifecycleFailFast() {
        LcaLifting tree = new LcaLifting(4, 0);
        assertThatThrownBy(() -> tree.lca(1, 2)).isInstanceOf(IllegalArgumentException.class);
        tree.addEdge(0, 1);
        tree.addEdge(1, 2);
        assertThatThrownBy(() -> tree.addEdge(0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.addEdge(3, 0)).isInstanceOf(IllegalArgumentException.class);
        tree.addEdge(1, 3);
        tree.build();
        assertThatThrownBy(() -> tree.addEdge(2, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.kthAncestor(3, 9)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.lca(0, 99)).isInstanceOf(IllegalArgumentException.class);
    }

    private int walkLca(int[] parent, int u, int v) {
        int du = walkDepth(parent, u);
        int dv = walkDepth(parent, v);
        while (du > dv) {
            u = parent[u];
            du--;
        }
        while (dv > du) {
            v = parent[v];
            dv--;
        }
        while (u != v) {
            u = parent[u];
            v = parent[v];
        }
        return u;
    }

    private int walkDepth(int[] parent, int u) {
        int d = 0;
        while (u != 0) {
            u = parent[u];
            d++;
        }
        return d;
    }
}
