package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BronKerboschCliquesTest {

    @Test
    void shouldMatchClassicCliqueAnchors() {
        // K4：唯一极大团 {0,1,2,3}
        int[][] k4 = {{0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}};
        List<int[]> cliques = BronKerboschCliques.maximalCliques(4, k4);
        assertThat(cliques).hasSize(1);
        assertThat(cliques.get(0)).containsExactly(0, 1, 2, 3);
        // C5 五环：无三角，极大团 = 5 条边
        int[][] c5 = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 0}};
        List<int[]> edges = BronKerboschCliques.maximalCliques(5, c5);
        assertThat(edges).hasSize(5);
        assertThat(edges.get(0)).hasSize(2);
        // 重复边归一：同 K4
        assertThat(BronKerboschCliques.maximalCliques(4,
                new int[][]{{0, 1}, {1, 0}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}})).hasSize(1);
    }

    @Test
    void shouldHandleDegenerateShapes() {
        // 空图 n=3：三个单点极大团
        List<int[]> singles = BronKerboschCliques.maximalCliques(3, new int[][]{});
        assertThat(singles).hasSize(3);
        assertThat(singles.get(0)).containsExactly(0);
        // n=0：零团
        assertThat(BronKerboschCliques.maximalCliques(0, new int[][]{})).isEmpty();
        // 三角+垂耳：{0,1,2} 团 + {2,3} 边团
        List<int[]> pendant = BronKerboschCliques.maximalCliques(4,
                new int[][]{{0, 1}, {1, 2}, {0, 2}, {2, 3}});
        assertThat(pendant).hasSize(2);
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        int[][] graph = {{0, 1}, {1, 2}, {0, 2}, {2, 3}, {3, 4}, {2, 4}};
        assertThat(sameContent(BronKerboschCliques.maximalCliques(5, graph),
                BronKerboschCliques.maximalCliques(5, graph))).isTrue();
        assertThatThrownBy(() -> BronKerboschCliques.maximalCliques(2, new int[][]{{0, 0}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BronKerboschCliques.maximalCliques(2, new int[][]{{0, 2}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BronKerboschCliques.maximalCliques(-1, new int[][]{}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldOutputOnlyMaximalCliquesOnRandomGraphs() {
        // 随机圣像：每输出确为团（两两相邻）且极大（任加一点破坏团性）；确定性双跑
        Random random = new Random(23);
        for (int t = 0; t < 30; t++) {
            int n = 2 + random.nextInt(8);
            int m = random.nextInt(20);
            boolean[][] adj = new boolean[n][n];
            int[][] es = new int[m][];
            for (int i = 0; i < m; i++) {
                int u = random.nextInt(n);
                int v = random.nextInt(n);
                if (u == v) {
                    v = (v + 1) % n;
                }
                es[i] = new int[]{u, v};
                adj[u][v] = true;
                adj[v][u] = true;
            }
            List<int[]> cliques = BronKerboschCliques.maximalCliques(n, es);
            assertThat(sameContent(cliques, BronKerboschCliques.maximalCliques(n, es))).isTrue();
            for (int[] clique : cliques) {
                for (int i = 0; i < clique.length; i++) {
                    for (int j = i + 1; j < clique.length; j++) {
                        assertThat(adj[clique[i]][clique[j]]).as("团性").isTrue();
                    }
                }
                boolean maximal = false;
                for (int v = 0; v < n && !maximal; v++) {
                    boolean inside = false;
                    for (int c : clique) {
                        if (c == v) {
                            inside = true;
                            break;
                        }
                    }
                    if (inside) {
                        continue;
                    }
                    boolean connectsAll = true;
                    for (int c : clique) {
                        if (!adj[v][c]) {
                            connectsAll = false;
                            break;
                        }
                    }
                    if (connectsAll) {
                        maximal = true;
                    }
                }
                assertThat(maximal).as("团 %s 必极大", java.util.Arrays.toString(clique)).isFalse();
            }
        }
    }

    private static boolean sameContent(List<int[]> a, List<int[]> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!java.util.Arrays.equals(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }
}
