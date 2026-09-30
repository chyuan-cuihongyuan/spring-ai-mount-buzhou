package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AStarSearchTest {

    @Test
    void shouldMatchClassicAnchors() {
        // 手锚：菱形网 0-1(1)/0-2(4)/1-2(2)/1-3(5)/2-3(1)——0→3 最短 4（0-1-2-3）
        int[][] edges = {{0, 1, 1}, {0, 2, 4}, {1, 2, 2}, {1, 3, 5}, {2, 3, 1}};
        assertThat(AStarSearch.shortestPath(4, edges, 0, 3, n -> 0)).isEqualTo(4);
        // 曼哈顿网格启发：3×3 网格 0→8 最短 4（四邻单位权）
        java.util.List<int[]> grid = new java.util.ArrayList<>();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int id = r * 3 + c;
                if (c < 2) {
                    grid.add(new int[]{id, id + 1, 1});
                }
                if (r < 2) {
                    grid.add(new int[]{id, id + 3, 1});
                }
            }
        }
        int[][] gridEdges = grid.toArray(new int[0][]);
        java.util.function.ToIntFunction<Integer> manhattan =
                n -> Math.abs(n / 3 - 2) + Math.abs(n % 3 - 2);
        assertThat(AStarSearch.shortestPath(9, gridEdges, 0, 8, manhattan)).isEqualTo(4);
        assertThat(AStarSearch.shortestPath(9, gridEdges, 0, 8, n -> 0)).isEqualTo(4); // h=0 退化 Dijkstra
        // 源=汇
        assertThat(AStarSearch.shortestPath(4, edges, 2, 2, n -> 0)).isZero();
        // 完美启发（h=真值）直达
        java.util.function.ToIntFunction<Integer> perfect = n -> n == 3 ? 4 : (n == 1 ? 5 : n == 2 ? 1 : 4);
        assertThat(AStarSearch.shortestPath(4, edges, 0, 3, perfect)).isEqualTo(4);
    }

    @Test
    void shouldAgreeWithDijkstraOnRandomGraphs() {
        // 互证圣像：随机图（含曼哈顿风格可采纳 h）与 Dijkstra 全等
        Random random = new Random(223);
        for (int t = 0; t < 60; t++) {
            int n = 3 + random.nextInt(10);
            int m = random.nextInt(25);
            int[][] edges = new int[m][];
            for (int i = 0; i < m; i++) {
                int u = random.nextInt(n);
                int v = random.nextInt(n);
                if (u == v) {
                    v = (v + 1) % n;
                }
                edges[i] = new int[]{u, v, 1 + random.nextInt(9)};
            }
            int target = n - 1;
            // 可采纳 h：随机固定 0..8 的低估（≤ 真值无保证——用 0 与 1 保守低估）
            java.util.function.ToIntFunction<Integer> h = n2 -> random.nextInt(2);
            DijkstraShortestPath dijkstra = new DijkstraShortestPath(n);
            for (int[] e : edges) {
                // Dijkstra 有向边——双向加边对齐 A* 的无向口径
                dijkstra.addEdge(e[0], e[1], e[2]);
                dijkstra.addEdge(e[1], e[0], e[2]);
            }
            long[] expected = dijkstra.distancesFrom(0);
            try {
                long actual = AStarSearch.shortestPath(n, edges, 0, target, h);
                assertThat(actual).as("图 %d 0→%d", t, target).isEqualTo(expected[target]);
            } catch (IllegalArgumentException unreachable) {
                assertThat(expected[target]).as("图 %d 不可达一致（Dijkstra 约定 -1）", t).isEqualTo(-1L);
            }
        }
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        int[][] edges = {{0, 1, 2}, {1, 2, 3}};
        assertThat(AStarSearch.shortestPath(3, edges, 0, 2, n -> 0))
                .isEqualTo(AStarSearch.shortestPath(3, edges, 0, 2, n -> 0));
        assertThatThrownBy(() -> AStarSearch.shortestPath(0, edges, 0, 0, n -> 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AStarSearch.shortestPath(3, edges, 3, 0, n -> 0))
                .hasMessageContaining("端点越域");
        assertThatThrownBy(() -> AStarSearch.shortestPath(3, edges, 0, 2, null))
                .hasMessageContaining("启发函数");
        assertThatThrownBy(() -> AStarSearch.shortestPath(3, new int[][]{{0, 1, -2}}, 0, 1, n -> 0))
                .hasMessageContaining("权非负");
        // 断图不可达 fail-fast（诚实拒绝而非 ∞ 返回）
        assertThatThrownBy(() -> AStarSearch.shortestPath(3, new int[][]{{0, 1, 1}}, 0, 2, n -> 0))
                .hasMessageContaining("不可达");
    }
}
