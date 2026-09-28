package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7007：FloydWarshall 合同——全对中转闭包。手锚；
 * 非负随机图 vs Dijkstra 逐行圣像；负权；负环 fail-fast；
 * 多边折叠；fail-fast。
 */
class FloydWarshallTest {

    @Test
    void handAnchoredClosure() {
        FloydWarshall fw = new FloydWarshall(4);
        fw.addEdge(0, 1, 5);
        fw.addEdge(1, 2, 3);
        fw.addEdge(0, 2, 10);
        fw.addEdge(2, 3, 1);
        fw.addEdge(0, 3, 100);
        long[][] d = fw.allDistances();
        assertThat(d[0][3]).isEqualTo(9);
        assertThat(d[0][2]).isEqualTo(8);
        assertThat(d[3][0]).isEqualTo(FloydWarshall.UNREACHABLE);
        assertThat(d[1][0]).isEqualTo(FloydWarshall.UNREACHABLE);
        assertThat(d[2][2]).isZero();
        assertThat(fw.edgeCount()).isEqualTo(5);
    }

    @Test
    void randomGraphsMatchDijkstraRows() {
        Random rng = new Random(7007L);
        for (int g = 0; g < 60; g++) {
            int n = 2 + rng.nextInt(12);
            FloydWarshall fw = new FloydWarshall(n);
            DijkstraShortestPath dsp = new DijkstraShortestPath(n);
            int edges = rng.nextInt(3 * n);
            for (int e = 0; e < edges; e++) {
                int from = rng.nextInt(n);
                int to = rng.nextInt(n);
                long w = rng.nextInt(40);
                fw.addEdge(from, to, w);
                dsp.addEdge(from, to, w);
            }
            long[][] all = fw.allDistances();
            for (int s = 0; s < n; s++) {
                long[] dijkstraRow = dsp.distancesFrom(s);
                for (int j = 0; j < n; j++) {
                    if (dijkstraRow[j] == -1L) {
                        dijkstraRow[j] = FloydWarshall.UNREACHABLE;
                    }
                }
                assertThat(all[s])
                        .as("图 %d 行 %d", g, s)
                        .containsExactly(dijkstraRow);
            }
        }
    }

    @Test
    void negativeEdgesAndNegativeCycle() {
        FloydWarshall fw = new FloydWarshall(3);
        fw.addEdge(0, 1, 2);
        fw.addEdge(1, 2, -5);
        long[][] d = fw.allDistances();
        assertThat(d[0][2]).isEqualTo(-3);

        FloydWarshall bad = new FloydWarshall(3);
        bad.addEdge(0, 1, 1);
        bad.addEdge(1, 2, -1);
        bad.addEdge(2, 1, -1);
        assertThatThrownBy(bad::allDistances).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multiEdgeCollapseAndFailFast() {
        FloydWarshall fw = new FloydWarshall(2);
        fw.addEdge(0, 1, 7);
        fw.addEdge(0, 1, 3);
        assertThat(fw.edgeCount()).isEqualTo(1);
        assertThat(fw.allDistances()[0][1]).isEqualTo(3);
        assertThatThrownBy(() -> new FloydWarshall(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> fw.addEdge(0, 2, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
