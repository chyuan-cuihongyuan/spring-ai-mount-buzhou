package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 10048 / X10097：JumpPointSearch 合同验证——走廊/绕障手锚+切角允许面
 * （原 paper 口径勘误入档）+随机格与网格 Dijkstra 等价圣像+不可达+确定性
 * +fail-fast。
 */
class JumpPointSearchTest {

    /** 网格 Dijkstra 神像（octile 权+同禁切角规则）。 */
    private static double gridDijkstra(boolean[][] walkable, int[] start, int[] goal) {
        int rows = walkable.length;
        int cols = walkable[0].length;
        double[][] dist = new double[rows][cols];
        for (double[] row : dist) {
            java.util.Arrays.fill(row, Double.MAX_VALUE);
        }
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1},
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        dist[start[0]][start[1]] = 0;
        PriorityQueue<double[]> heap = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        heap.add(new double[]{0, start[0], start[1]});
        while (!heap.isEmpty()) {
            double[] top = heap.poll();
            int r = (int) top[1];
            int c = (int) top[2];
            if (top[0] > dist[r][c]) {
                continue;
            }
            for (int[] d : dirs) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || !walkable[nr][nc]) {
                    continue;
                }
                // 原 paper 切角允许口径：斜移只要求目标格可走
                double step = d[0] != 0 && d[1] != 0 ? Math.sqrt(2) : 1.0;
                if (top[0] + step < dist[nr][nc]) {
                    dist[nr][nc] = top[0] + step;
                    heap.add(new double[]{dist[nr][nc], nr, nc});
                }
            }
        }
        return dist[goal[0]][goal[1]];
    }

    private static double pathCost(List<int[]> path) {
        double cost = 0;
        for (int i = 1; i < path.size(); i++) {
            int dr = Math.abs(path.get(i)[0] - path.get(i - 1)[0]);
            int dc = Math.abs(path.get(i)[1] - path.get(i - 1)[1]);
            assertThat(Math.max(dr, dc)).as("步 %d 为相邻格", i).isEqualTo(1);
            cost += dr + dc == 2 ? Math.sqrt(2) : 1.0;
        }
        return cost;
    }

    @Test
    void shouldWalkStraightCorridor_whenOpenGrid() {
        boolean[][] grid = new boolean[1][10];
        for (boolean[] row : grid) {
            java.util.Arrays.fill(row, true);
        }
        List<int[]> path = JumpPointSearch.path(grid, new int[]{0, 0}, new int[]{0, 9});
        assertThat(path).hasSize(10);
        assertThat(path.get(0)).containsExactly(0, 0);
        assertThat(path.get(9)).containsExactly(0, 9);
    }

    @Test
    void shouldDetourAroundWall_whenLShapedObstacle() {
        boolean[][] grid = new boolean[3][3];
        for (boolean[] row : grid) {
            java.util.Arrays.fill(row, true);
        }
        grid[1][1] = false; // 中心墙
        List<int[]> path = JumpPointSearch.path(grid, new int[]{1, 0}, new int[]{1, 2});
        assertThat(pathCost(path)).isCloseTo(2 * Math.sqrt(2), within(1e-9));
    }

    @Test
    void shouldCutDiagonalCorner_whenOriginalPaperSemantics() {
        boolean[][] grid = new boolean[3][3];
        for (boolean[] row : grid) {
            java.util.Arrays.fill(row, true);
        }
        grid[0][1] = false;
        grid[1][0] = false;
        // (1,1)→(0,0)：原 paper 切角允许——斜步直达，代价 √2
        List<int[]> path = JumpPointSearch.path(grid, new int[]{1, 1}, new int[]{0, 0});
        assertThat(path).hasSize(2);
        assertThat(pathCost(path)).isCloseTo(Math.sqrt(2), within(1e-9));
    }

    @Test
    void shouldMatchDijkstraCost_whenRandomGrids() {
        Random random = new Random(10048L);
        for (int trial = 0; trial < 20; trial++) {
            int rows = 12;
            int cols = 12;
            boolean[][] grid = new boolean[rows][cols];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    grid[r][c] = random.nextDouble() > 0.25;
                }
            }
            grid[0][0] = true;
            grid[rows - 1][cols - 1] = true;
            List<int[]> path = JumpPointSearch.path(grid,
                    new int[]{0, 0}, new int[]{rows - 1, cols - 1});
            double oracle = gridDijkstra(grid,
                    new int[]{0, 0}, new int[]{rows - 1, cols - 1});
            if (oracle == Double.MAX_VALUE) {
                assertThat(path).as("随机格 %d 不可达", trial).isEmpty();
            } else {
                assertThat(pathCost(path)).as("随机格 %d 代价与 Dijkstra 相等", trial)
                        .isCloseTo(oracle, within(1e-9));
            }
        }
    }

    @Test
    void shouldReturnSinglePoint_whenStartEqualsGoal() {
        boolean[][] grid = new boolean[4][4];
        for (boolean[] row : grid) {
            java.util.Arrays.fill(row, true);
        }
        List<int[]> path = JumpPointSearch.path(grid, new int[]{2, 2}, new int[]{2, 2});
        assertThat(path).hasSize(1);
    }

    @Test
    void shouldReproduceIdenticalPath_whenSameInputTwice() {
        boolean[][] grid = new boolean[6][6];
        for (boolean[] row : grid) {
            java.util.Arrays.fill(row, true);
        }
        grid[2][2] = false;
        grid[3][3] = false;
        List<int[]> first = JumpPointSearch.path(grid, new int[]{0, 0}, new int[]{5, 5});
        List<int[]> second = JumpPointSearch.path(grid, new int[]{0, 0}, new int[]{5, 5});
        assertThat(second).hasSameSizeAs(first);
        for (int i = 0; i < first.size(); i++) {
            assertThat(second.get(i)).containsExactly(first.get(i));
        }
    }

    @Test
    void shouldFailFast_whenNullOrUnwalkableEnds() {
        assertThatThrownBy(() -> JumpPointSearch.path(null, new int[]{0, 0}, new int[]{1, 1}))
                .isInstanceOf(IllegalArgumentException.class);
        boolean[][] grid = new boolean[3][3];
        for (boolean[] row : grid) {
            java.util.Arrays.fill(row, true);
        }
        grid[2][2] = false;
        assertThatThrownBy(() -> JumpPointSearch.path(grid, new int[]{0, 0}, new int[]{2, 2}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("终点不可走");
        assertThatThrownBy(() -> JumpPointSearch.path(grid, new int[]{5, 5}, new int[]{0, 0}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越界");
    }
}
