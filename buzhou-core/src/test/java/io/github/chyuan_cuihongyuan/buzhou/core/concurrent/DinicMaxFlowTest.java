package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DinicMaxFlowTest {

    @Test
    void shouldMatchHandAnchors() {
        int[][] clrs = {
                {0, 1, 16}, {0, 2, 13}, {1, 2, 10}, {1, 3, 12}, {2, 1, 4},
                {2, 4, 14}, {3, 2, 9}, {3, 5, 20}, {4, 3, 7}, {4, 5, 4}};
        assertThat(DinicMaxFlow.maxFlow(6, clrs, 0, 5)).isEqualTo(23);
        assertThat(DinicMaxFlow.maxFlow(3, new int[][]{{0, 1, 3}, {1, 2, 2}}, 0, 2)).isEqualTo(2);
        assertThat(DinicMaxFlow.maxFlow(4, new int[][]{{0, 1, 1}, {0, 2, 1}, {1, 3, 1}, {2, 3, 1}}, 0, 3))
                .isEqualTo(2);
        assertThat(DinicMaxFlow.maxFlow(2, new int[][]{{0, 1, 5}}, 0, 1)).isEqualTo(5);
        assertThat(DinicMaxFlow.maxFlow(2, new int[0][], 0, 1)).isEqualTo(0);
    }

    @Test
    void shouldMatchMinCutOracleOnRandomGraphs() {
        Random random = new Random(8006);
        for (int round = 0; round < 100; round++) {
            int n = 3 + random.nextInt(6);
            int m = random.nextInt(2 * n + 2);
            int[][] edges = new int[m][3];
            for (int i = 0; i < m; i++) {
                edges[i][0] = random.nextInt(n);
                edges[i][1] = random.nextInt(n);
                edges[i][2] = 1 + random.nextInt(9);
            }
            int source = 0;
            int sink = n - 1;
            long expected = bruteMinCut(n, edges, source, sink);
            assertThat(DinicMaxFlow.maxFlow(n, edges, source, sink))
                    .as("round=%d n=%d", round, n)
                    .isEqualTo(expected);
        }
    }

    @Test
    void shouldFailFastOnBadGraphs() {
        assertThatThrownBy(() -> DinicMaxFlow.maxFlow(0, new int[0][], 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DinicMaxFlow.maxFlow(2, new int[][]{{0, 1, 1}}, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DinicMaxFlow.maxFlow(2, new int[][]{{0, 1, 1}}, 0, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DinicMaxFlow.maxFlow(2, new int[][]{{0, 1, -1}}, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DinicMaxFlow.maxFlow(2, new int[][]{{0, 5, 1}}, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 圣像：最大流=最小割——枚举含源不含汇的全部子集聚合跨割容量取最小。 */
    private static long bruteMinCut(int n, int[][] edges, int source, int sink) {
        long best = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << n); mask++) {
            if (((mask >> source) & 1) == 0 || ((mask >> sink) & 1) == 1) {
                continue;
            }
            long cut = 0;
            for (int[] edge : edges) {
                boolean fromIn = ((mask >> edge[0]) & 1) == 1;
                boolean toIn = ((mask >> edge[1]) & 1) == 1;
                if (fromIn && !toIn) {
                    cut += edge[2];
                }
            }
            best = Math.min(best, cut);
        }
        return best;
    }
}
