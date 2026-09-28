package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7006：BellmanFord 合同——负权单源+负环检测。
 * 负权手锚；不可达 -1；可达负环 fail-fast；全图负环探测；
 * 非负随机图 vs Dijkstra 圣像；fail-fast。
 */
class BellmanFordTest {

    @Test
    void negativeEdgesHandAnchors() {
        BellmanFord bf = new BellmanFord(5);
        bf.addEdge(0, 1, 4);
        bf.addEdge(0, 2, 5);
        bf.addEdge(1, 3, -7);
        bf.addEdge(2, 3, 2);
        bf.addEdge(3, 4, 1);
        long[] dist = bf.distancesFrom(0);
        assertThat(dist).containsExactly(0L, 4L, 5L, -3L, -2L);
        assertThat(bf.edgeCount()).isEqualTo(5);
    }

    @Test
    void unreachableAreHonestMinusOne() {
        BellmanFord bf = new BellmanFord(3);
        bf.addEdge(1, 2, 5);
        assertThat(bf.distancesFrom(0))
                .containsExactly(0L, BellmanFord.UNREACHABLE, BellmanFord.UNREACHABLE);
    }

    @Test
    void reachableNegativeCycleFailsFast() {
        BellmanFord bf = new BellmanFord(3);
        bf.addEdge(0, 1, 1);
        bf.addEdge(1, 2, -1);
        bf.addEdge(2, 1, -1);
        assertThatThrownBy(() -> bf.distancesFrom(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unreachableNegativeCycleDetectedGlobally() {
        BellmanFord bf = new BellmanFord(4);
        bf.addEdge(0, 1, 1);
        bf.addEdge(2, 3, -1);
        bf.addEdge(3, 2, -1);
        long[] dist = bf.distancesFrom(0);
        assertThat(dist).containsExactly(0L, 1L,
                BellmanFord.UNREACHABLE, BellmanFord.UNREACHABLE);
        assertThat(bf.hasNegativeCycle()).isTrue();
    }

    @Test
    void nonNegativeRandomGraphsMatchDijkstra() {
        Random rng = new Random(7006L);
        for (int g = 0; g < 100; g++) {
            int n = 2 + rng.nextInt(15);
            BellmanFord bf = new BellmanFord(n);
            DijkstraShortestPath dsp = new DijkstraShortestPath(n);
            int edges = rng.nextInt(3 * n);
            for (int e = 0; e < edges; e++) {
                int from = rng.nextInt(n);
                int to = rng.nextInt(n);
                long w = rng.nextInt(50);
                bf.addEdge(from, to, w);
                dsp.addEdge(from, to, w);
            }
            int source = rng.nextInt(n);
            long[] dijkstra = dsp.distancesFrom(source);
            for (int j = 0; j < n; j++) {
                if (dijkstra[j] == -1L) {
                    dijkstra[j] = BellmanFord.UNREACHABLE;
                }
            }
            assertThat(bf.distancesFrom(source))
                    .as("图 %d source %d", g, source)
                    .containsExactly(dijkstra);
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new BellmanFord(0)).isInstanceOf(IllegalArgumentException.class);
        BellmanFord bf = new BellmanFord(2);
        assertThatThrownBy(() -> bf.addEdge(0, 2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bf.distancesFrom(2)).isInstanceOf(IllegalArgumentException.class);
    }
}
