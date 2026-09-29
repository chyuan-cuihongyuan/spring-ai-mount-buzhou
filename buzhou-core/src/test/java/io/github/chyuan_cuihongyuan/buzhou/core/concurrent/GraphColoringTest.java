package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraphColoringTest {

    @Test
    void shouldMatchHandAnchors() {
        int[][] cycle4 = {{0, 1}, {1, 2}, {2, 3}, {3, 0}};
        assertThat(GraphColoring.colorCount(GraphColoring.colors(4, cycle4))).isEqualTo(2);
        int[][] cycle5 = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 0}};
        assertThat(GraphColoring.colorCount(GraphColoring.colors(5, cycle5))).isEqualTo(3);
        int[][] clique4 = complete(4);
        assertThat(GraphColoring.colorCount(GraphColoring.colors(4, clique4))).isEqualTo(4);
        int[][] star = {{0, 1}, {0, 2}, {0, 3}, {0, 4}};
        assertThat(GraphColoring.colorCount(GraphColoring.colors(5, star))).isEqualTo(2);
        assertThat(GraphColoring.colors(3, new int[0][])).containsExactly(0, 0, 0);
        assertThat(GraphColoring.colors(4, cycle4)).containsExactly(0, 1, 0, 1);
    }

    @Test
    void shouldHoldFeasibilityAndDegreeBoundOnRandomGraphs() {
        Random random = new Random(8010);
        for (int round = 0; round < 200; round++) {
            int n = 2 + random.nextInt(8);
            int m = random.nextInt(2 * n + 2);
            int[][] edges = new int[m][2];
            for (int i = 0; i < m; i++) {
                edges[i][0] = random.nextInt(n);
                edges[i][1] = random.nextInt(n);
                if (edges[i][0] == edges[i][1]) {
                    edges[i][1] = (edges[i][1] + 1) % n;
                }
            }
            int[] colors = GraphColoring.colors(n, edges);
            int[] colorsAgain = GraphColoring.colors(n, edges);
            assertThat(colors).as("round=%d 确定性", round).isEqualTo(colorsAgain);
            int maxDegree = 0;
            int[] degree = new int[n];
            for (int[] edge : edges) {
                degree[edge[0]]++;
                degree[edge[1]]++;
            }
            for (int d : degree) {
                maxDegree = Math.max(maxDegree, d);
            }
            assertThat(GraphColoring.colorCount(colors)).isLessThanOrEqualTo(maxDegree + 2);
            for (int[] edge : edges) {
                assertThat(colors[edge[0]]).as("round=%d 邻异色", round)
                        .isNotEqualTo(colors[edge[1]]);
            }
        }
    }

    @Test
    void shouldFailFastOnSelfLoopAndOutOfRange() {
        assertThatThrownBy(() -> GraphColoring.colors(2, new int[][]{{1, 1}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GraphColoring.colors(2, new int[][]{{0, 7}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GraphColoring.colors(0, new int[0][]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GraphColoring.colorCount(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static int[][] complete(int n) {
        int[][] edges = new int[n * (n - 1) / 2][2];
        int idx = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                edges[idx][0] = i;
                edges[idx][1] = j;
                idx++;
            }
        }
        return edges;
    }
}
