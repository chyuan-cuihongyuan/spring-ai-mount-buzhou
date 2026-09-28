package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6049：DijkstraShortestPath 合同——非负权单源最短路。
 * 经典图锚值；随机图 vs Bellman-Ford 圣像（200 图逐步全等）；
 * 不可达 -1；零权边；确定性；fail-fast。
 */
class DijkstraShortestPathTest {

    @Test
    void classicGraphAnchors() {
        DijkstraShortestPath dsp = new DijkstraShortestPath(5);
        dsp.addEdge(0, 1, 10);
        dsp.addEdge(0, 2, 3);
        dsp.addEdge(1, 2, 1);
        dsp.addEdge(2, 1, 4);
        dsp.addEdge(1, 3, 2);
        dsp.addEdge(2, 3, 8);
        dsp.addEdge(3, 4, 7);
        dsp.addEdge(2, 4, 2);
        long[] dist = dsp.distancesFrom(0);
        assertThat(dist).containsExactly(0L, 7L, 3L, 9L, 5L);
        assertThat(dsp.nodeCount()).isEqualTo(5);
        assertThat(dsp.edgeCount()).isEqualTo(8);
    }

    @Test
    void unreachableNodesAreHonestMinusOne() {
        DijkstraShortestPath dsp = new DijkstraShortestPath(4);
        dsp.addEdge(1, 2, 5);
        long[] dist = dsp.distancesFrom(0);
        assertThat(dist).containsExactly(0L, -1L, -1L, -1L);
        long[] fromTwo = dsp.distancesFrom(2);
        assertThat(fromTwo).containsExactly(-1L, -1L, 0L, -1L);
    }

    @Test
    void zeroWeightEdgesAndSelfLoopHarmless() {
        DijkstraShortestPath dsp = new DijkstraShortestPath(3);
        dsp.addEdge(0, 0, 0);
        dsp.addEdge(0, 1, 0);
        dsp.addEdge(1, 2, 0);
        long[] dist = dsp.distancesFrom(0);
        assertThat(dist).containsExactly(0L, 0L, 0L);
    }

    @Test
    void randomGraphsMatchBellmanFordOracle() {
        Random rng = new Random(6049L);
        for (int g = 0; g < 200; g++) {
            int n = 2 + rng.nextInt(20);
            DijkstraShortestPath dsp = new DijkstraShortestPath(n);
            long[][] weight = new long[n][n];
            for (int i = 0; i < n; i++) {
                java.util.Arrays.fill(weight[i], -1L);
            }
            int edges = rng.nextInt(3 * n);
            for (int e = 0; e < edges; e++) {
                int from = rng.nextInt(n);
                int to = rng.nextInt(n);
                long w = rng.nextInt(50);
                dsp.addEdge(from, to, w);
                weight[from][to] = weight[from][to] == -1L ? w : Math.min(weight[from][to], w);
            }
            int source = rng.nextInt(n);
            long[] dist = dsp.distancesFrom(source);
            long[] oracle = bellmanFord(weight, source, n);
            assertThat(dist)
                    .as("图 %d（n=%d, source=%d）", g, n, source)
                    .containsExactly(oracle);
        }
    }

    @Test
    void sameGraphSameResultDeterminism() {
        for (int r = 0; r < 2; r++) {
            DijkstraShortestPath dsp = new DijkstraShortestPath(6);
            dsp.addEdge(0, 1, 4);
            dsp.addEdge(0, 2, 1);
            dsp.addEdge(2, 1, 1);
            dsp.addEdge(1, 3, 1);
            dsp.addEdge(2, 3, 5);
            dsp.addEdge(3, 4, 3);
            dsp.addEdge(4, 5, 1);
            dsp.addEdge(0, 5, 100);
            assertThat(dsp.distancesFrom(0))
                    .containsExactly(0L, 2L, 1L, 3L, 6L, 7L);
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new DijkstraShortestPath(0))
                .isInstanceOf(IllegalArgumentException.class);
        DijkstraShortestPath dsp = new DijkstraShortestPath(3);
        assertThatThrownBy(() -> dsp.addEdge(0, 3, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> dsp.addEdge(-1, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> dsp.addEdge(0, 1, -2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> dsp.distancesFrom(5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Bellman-Ford 圣像（含 -1 不可达语义对齐）。 */
    private long[] bellmanFord(long[][] weight, int source, int n) {
        long[] dist = new long[n];
        java.util.Arrays.fill(dist, -1L);
        dist[source] = 0L;
        for (int round = 0; round < n; round++) {
            boolean changed = false;
            for (int u = 0; u < n; u++) {
                if (dist[u] == -1L) {
                    continue;
                }
                for (int v = 0; v < n; v++) {
                    if (weight[u][v] == -1L) {
                        continue;
                    }
                    long candidate = dist[u] + weight[u][v];
                    if (dist[v] == -1L || candidate < dist[v]) {
                        dist[v] = candidate;
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
        return dist;
    }
}
