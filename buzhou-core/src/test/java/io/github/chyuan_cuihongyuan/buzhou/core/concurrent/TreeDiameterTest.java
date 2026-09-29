package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TreeDiameterTest {

    @Test
    void shouldMatchHandAnchors() {
        assertThat(TreeDiameter.diameter(new int[]{-1})).isEqualTo(0);
        assertThat(TreeDiameter.diameter(new int[]{-1, 0, 1, 2, 3})).isEqualTo(4);
        assertThat(TreeDiameter.path(new int[]{-1, 0, 1, 2, 3})).containsExactly(4, 3, 2, 1, 0);
        assertThat(TreeDiameter.diameter(new int[]{-1, 0, 0, 0, 0})).isEqualTo(2);
        assertThat(TreeDiameter.path(new int[]{-1, 0, 0, 0})).containsExactly(1, 0, 2);
        assertThat(TreeDiameter.diameter(new int[]{-1, 0, 0})).isEqualTo(2);
        int[][] balanced = balancedBinary(7);
        assertThat(TreeDiameter.diameter(parentOf(balanced, 7))).isEqualTo(4);
    }

    @Test
    void shouldMatchAllPairsBfsOracleOnRandomTrees() {
        Random random = new Random(8009);
        for (int round = 0; round < 200; round++) {
            int n = 1 + random.nextInt(9);
            int[] parent = new int[n];
            parent[0] = -1;
            for (int node = 1; node < n; node++) {
                parent[node] = random.nextInt(node);
            }
            int expected = bruteDiameter(parent);
            int actual = TreeDiameter.diameter(parent);
            assertThat(actual).as("round=%d n=%d", round, n).isEqualTo(expected);
            List<Integer> path = TreeDiameter.path(parent);
            assertThat(path.size() - 1).isEqualTo(expected);
        }
    }

    @Test
    void shouldFailFastOnNonTrees() {
        assertThatThrownBy(() -> TreeDiameter.diameter(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TreeDiameter.diameter(new int[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TreeDiameter.diameter(new int[]{0}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TreeDiameter.diameter(new int[]{-1, 2, 1}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static int[] parentOf(int[][] edges, int n) {
        int[] parent = new int[n];
        java.util.Arrays.fill(parent, -1);
        for (int[] edge : edges) {
            parent[edge[1]] = edge[0];
        }
        return parent;
    }

    private static int[][] balancedBinary(int n) {
        java.util.List<int[]> edges = new java.util.ArrayList<>();
        for (int node = 1; node < n; node++) {
            edges.add(new int[]{(node - 1) / 2, node});
        }
        return edges.toArray(new int[0][]);
    }

    /** 圣像：全点对距离（沿父链）取 max。 */
    private static int bruteDiameter(int[] parent) {
        int n = parent.length;
        int[][] dist = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int d = 0;
                int[] depthOf = new int[n];
                for (int node = 0; node < n; node++) {
                    depthOf[node] = node == 0 ? 0 : depthOf[parent[node]] + 1;
                }
                int a = i;
                int b = j;
                while (a != b) {
                    if (depthOf[a] >= depthOf[b]) {
                        a = parent[a];
                        d++;
                    }
                    if (a != b && depthOf[b] >= depthOf[a]) {
                        b = parent[b];
                        d++;
                    }
                }
                dist[i][j] = d;
            }
        }
        int best = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                best = Math.max(best, dist[i][j]);
            }
        }
        return best;
    }
}
