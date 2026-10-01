package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 10034 / X10069：DominatorTree 合同验证——CFG 手锚+链图手锚+不可达 −1
 * +随机图与暴力删除法（删点判可达）全等圣像+确定性+fail-fast。
 */
class DominatorTreeTest {

    /** 暴力删除法神像：u 支配 v ⟺ 删 u 后 v 自 root 不可达。 */
    private static int[] bruteForceIdom(int nodeCount, int[][] edges, int root) {
        List<Set<Integer>> dominators = new java.util.ArrayList<>();
        for (int v = 0; v < nodeCount; v++) {
            dominators.add(new HashSet<>());
        }
        for (int v = 0; v < nodeCount; v++) {
            if (v == root) {
                dominators.get(v).add(v);
                continue;
            }
            if (!reachable(nodeCount, edges, root, v, -1)) {
                continue;
            }
            dominators.get(v).add(root);
            dominators.get(v).add(v);
            for (int u = 0; u < nodeCount; u++) {
                if (u == root || u == v) {
                    continue;
                }
                if (!reachable(nodeCount, edges, root, v, u)) {
                    dominators.get(v).add(u);
                }
            }
        }
        int[] idom = new int[nodeCount];
        Arrays.fill(idom, -1);
        for (int v = 0; v < nodeCount; v++) {
            if (v == root) {
                idom[v] = root;
                continue;
            }
            Set<Integer> strict = new HashSet<>(dominators.get(v));
            strict.remove(v);
            int best = -1;
            for (int candidate : strict) {
                if (best == -1 || dominators.get(candidate).size()
                        > dominators.get(best).size()) {
                    best = candidate;
                }
            }
            idom[v] = best;
        }
        return idom;
    }

    /** 删 blocked 后 v 是否自 root 可达（BFS）。 */
    private static boolean reachable(int nodeCount, int[][] edges, int root, int target,
            int blocked) {
        if (blocked == root) {
            return false;
        }
        List<List<Integer>> successors = new java.util.ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            successors.add(new java.util.ArrayList<>());
        }
        for (int[] edge : edges) {
            successors.get(edge[0]).add(edge[1]);
        }
        boolean[] seen = new boolean[nodeCount];
        Deque<Integer> queue = new ArrayDeque<>();
        seen[root] = true;
        queue.add(root);
        while (!queue.isEmpty()) {
            int vertex = queue.poll();
            if (vertex == target) {
                return true;
            }
            for (int to : successors.get(vertex)) {
                if (!seen[to] && to != blocked) {
                    seen[to] = true;
                    queue.add(to);
                }
            }
        }
        return false;
    }

    @Test
    void shouldMatchHandAnchor_whenCfgWithJoinAndBackEdge() {
        // 0→1,0→2,1→3,2→3,3→4,4→1,4→5：join 点 3 的 idom=0（菱形收敛）
        int[][] edges = {{0, 1}, {0, 2}, {1, 3}, {2, 3}, {3, 4}, {4, 1}, {4, 5}};
        int[] idom = DominatorTree.immediateDominators(6, edges, 0);
        assertThat(idom).containsExactly(0, 0, 0, 0, 3, 4);
    }

    @Test
    void shouldChainParents_whenLinearGraph() {
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 4}};
        int[] idom = DominatorTree.immediateDominators(5, edges, 0);
        assertThat(idom).containsExactly(0, 0, 1, 2, 3);
    }

    @Test
    void shouldMarkUnreachableAsMinusOne_whenDisconnectedSubgraph() {
        int[][] edges = {{0, 1}, {2, 3}};
        int[] idom = DominatorTree.immediateDominators(4, edges, 0);
        assertThat(idom).containsExactly(0, 0, -1, -1);
    }

    @Test
    void shouldMatchBruteForce_whenRandomGraphs() {
        Random random = new Random(10034L);
        for (int trial = 0; trial < 20; trial++) {
            int n = 12;
            int[][] edges = new int[25][2];
            for (int[] edge : edges) {
                edge[0] = random.nextInt(n);
                edge[1] = random.nextInt(n);
            }
            int[] fast = DominatorTree.immediateDominators(n, edges, 0);
            int[] oracle = bruteForceIdom(n, edges, 0);
            assertThat(fast).as("随机图 %d 支配树与暴力删除法全等", trial).isEqualTo(oracle);
        }
    }

    @Test
    void shouldReproduceIdenticalTree_whenSameInputTwice() {
        int[][] edges = {{0, 1}, {0, 2}, {1, 3}, {2, 3}, {3, 4}, {4, 1}, {4, 5}};
        assertThat(DominatorTree.immediateDominators(6, edges, 0))
                .isEqualTo(DominatorTree.immediateDominators(6, edges, 0));
    }

    @Test
    void shouldFailFast_whenNullEdgesOrOutOfRange() {
        assertThatThrownBy(() -> DominatorTree.immediateDominators(4, null, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DominatorTree.immediateDominators(
                4, new int[][]{{0, 4}}, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越域");
        assertThatThrownBy(() -> DominatorTree.immediateDominators(
                4, new int[][]{{0, 1}}, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("root 越域");
        assertThatThrownBy(() -> DominatorTree.immediateDominators(0, new int[][]{}, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
