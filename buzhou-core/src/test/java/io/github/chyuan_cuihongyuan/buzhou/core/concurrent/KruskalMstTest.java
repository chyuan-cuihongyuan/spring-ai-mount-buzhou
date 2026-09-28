package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7008：KruskalMst 合同——贪心+并查集最小生成树。
 * 经典图总权锚；同权边确定性采纳；随机连通图 vs 测内
 * Prim 圣像总权全等；不连通/自环 fail-fast。
 */
class KruskalMstTest {

    @Test
    void classicGraphTotalWeightAnchors() {
        KruskalMst mst = new KruskalMst(6);
        mst.addEdge(0, 1, 6);
        mst.addEdge(0, 2, 1);
        mst.addEdge(0, 3, 5);
        mst.addEdge(1, 2, 5);
        mst.addEdge(1, 4, 3);
        mst.addEdge(2, 4, 6);
        mst.addEdge(2, 5, 4);
        mst.addEdge(3, 5, 2);
        mst.addEdge(4, 5, 6);
        assertThat(mst.totalWeight()).isEqualTo(15);
        assertThat(mst.minimumSpanningTree()).hasSize(5);
        assertThat(mst.minimumSpanningTree().get(0)).containsExactly(1L, 0L, 2L);
    }

    @Test
    void deterministicUnderSameWeightTies() {
        KruskalMst a = new KruskalMst(4);
        KruskalMst b = new KruskalMst(4);
        long[][] edges = {{0, 1, 2}, {1, 2, 2}, {2, 3, 2}, {0, 3, 2}};
        for (long[] e : edges) {
            a.addEdge((int) e[0], (int) e[1], e[2]);
            b.addEdge((int) e[0], (int) e[1], e[2]);
        }
        assertThat(deepLists(b.minimumSpanningTree()))
                .containsExactlyElementsOf(deepLists(a.minimumSpanningTree()));
        assertThat(a.minimumSpanningTree()).hasSize(3);
    }

    @Test
    void randomConnectedGraphsMatchPrimOracle() {
        Random rng = new Random(7008L);
        for (int g = 0; g < 100; g++) {
            int n = 2 + rng.nextInt(15);
            long[][] weight = new long[n][n];
            for (int i = 0; i < n; i++) {
                java.util.Arrays.fill(weight[i], -1L);
            }
            KruskalMst mst = new KruskalMst(n);
            for (int v = 1; v < n; v++) {
                int parent = rng.nextInt(v);
                long w = rng.nextInt(100);
                mst.addEdge(parent, v, w);
                weight[parent][v] = weight[parent][v] == -1 || w < weight[parent][v]
                        ? w : weight[parent][v];
                weight[v][parent] = weight[parent][v];
            }
            for (int e = 0; e < 2 * n; e++) {
                int a = rng.nextInt(n);
                int b = rng.nextInt(n);
                if (a == b) {
                    continue;
                }
                long w = rng.nextInt(100);
                mst.addEdge(a, b, w);
                weight[a][b] = weight[a][b] == -1 || w < weight[a][b] ? w : weight[a][b];
                weight[b][a] = weight[a][b];
            }
            long kruskal = mst.totalWeight();
            long prim = primOracle(weight, n);
            assertThat(kruskal).as("图 %d（n=%d）", g, n).isEqualTo(prim);
        }
    }

    @Test
    void disconnectedAndSelfLoopFailFast() {
        KruskalMst disconnected = new KruskalMst(4);
        disconnected.addEdge(0, 1, 1);
        disconnected.addEdge(2, 3, 1);
        assertThatThrownBy(disconnected::totalWeight).isInstanceOf(IllegalArgumentException.class);
        KruskalMst loop = new KruskalMst(3);
        assertThatThrownBy(() -> loop.addEdge(1, 1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new KruskalMst(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static java.util.List<java.util.List<Long>> deepLists(java.util.List<long[]> edges) {
        java.util.List<java.util.List<Long>> out = new java.util.ArrayList<>();
        for (long[] e : edges) {
            out.add(java.util.List.of(e[0], e[1], e[2]));
        }
        return out;
    }

    /** 测内 Prim 圣像（稠密矩阵 O(n²)，-1=无边；不连通 -1）。 */
    private long primOracle(long[][] weight, int n) {
        boolean[] in = new boolean[n];
        long[] best = new long[n];
        java.util.Arrays.fill(best, -1L);
        best[0] = 0;
        long total = 0;
        for (int round = 0; round < n; round++) {
            int pick = -1;
            for (int i = 0; i < n; i++) {
                if (!in[i] && best[i] != -1L && (pick == -1 || best[i] < best[pick])) {
                    pick = i;
                }
            }
            if (pick == -1) {
                return -1;
            }
            in[pick] = true;
            total += best[pick];
            for (int j = 0; j < n; j++) {
                if (!in[j] && weight[pick][j] != -1L
                        && (best[j] == -1L || weight[pick][j] < best[j])) {
                    best[j] = weight[pick][j];
                }
            }
        }
        return total;
    }
}
