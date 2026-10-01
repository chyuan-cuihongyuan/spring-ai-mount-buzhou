package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 跳点搜索网格寻路（spec 10048 / X10097 / impl 2451）——Harabor–Grastien 2011
 * 原 paper 口径（PathFinding.js/GameAIPro 同源；**允许切角**——斜移只要求
 * 目标格可走，勘误入档：禁切角域强制邻格需前向+斜前双链播种、复杂度超时
 * 预算换原 paper 口径——「不可自洽即换静脉」纪律）：**8 向均匀格上识别跳点
 * （直扫遇强制邻格/撞墙回退、斜扫递归直扫）+ 跳点集上 octile A***——对称
 * 路径爆炸的最优等价剪枝面（AStarSearch 已占异面：任意图 vs 均匀格加速）。
 *
 * <p>起点无剪枝（8 自然邻格伪跳点预播种——JPS 口径）。不可达返回空列表；
 * null/越界/起终点不可走 fail-fast；开放堆 (f,r,c) 确定序，复算可复现。
 */
public final class JumpPointSearch {

    /** 8 方向（dr,dc）：正交 4 向+对角 4 向。 */
    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    private JumpPointSearch() {
    }

    /**
     * 路径（起终点含，逐格 [r,c] 序列）；不可达为空列表。
     *
     * @param walkable 可走格阵
     * @param start 起点 {r,c}
     * @param goal 终点 {r,c}
     * @throws IllegalArgumentException null/越界/起终点不可走
     */
    public static List<int[]> path(boolean[][] walkable, int[] start, int[] goal) {
        validate(walkable, start, goal);
        int rows = walkable.length;
        int columns = walkable[0].length;
        if (start[0] == goal[0] && start[1] == goal[1]) {
            return List.of(new int[]{start[0], start[1]});
        }
        double[][] bestCost = new double[rows][columns];
        int[][] jumpFrom = new int[rows][columns];
        for (double[] row : bestCost) {
            Arrays.fill(row, Double.MAX_VALUE);
        }
        for (int[] row : jumpFrom) {
            Arrays.fill(row, -1);
        }
        // 堆条目 {f, g, r, c}
        PriorityQueue<double[]> open = new PriorityQueue<>((a, b) -> {
            if (a[0] != b[0]) {
                return Double.compare(a[0], b[0]);
            }
            if (a[2] != b[2]) {
                return Double.compare(a[2], b[2]);
            }
            return Double.compare(a[3], b[3]);
        });
        bestCost[start[0]][start[1]] = 0.0;
        open.add(new double[]{octile(start[0], start[1], goal), 0.0,
                start[0], start[1]});
        // 起点无剪枝（JPS 口径）：8 自然邻格伪跳点预播种
        for (int[] direction : DIRECTIONS) {
            int nr = start[0] + direction[0];
            int nc = start[1] + direction[1];
            if (!inGrid(rows, columns, nr, nc) || !walkable[nr][nc]) {
                continue;
            }
            double step = direction[0] != 0 && direction[1] != 0
                    ? Math.sqrt(2.0) : 1.0;
            if (step < bestCost[nr][nc]) {
                bestCost[nr][nc] = step;
                jumpFrom[nr][nc] = start[0] * columns + start[1];
                open.add(new double[]{step + octile(nr, nc, goal), step, nr, nc});
            }
        }
        while (!open.isEmpty()) {
            double[] current = open.poll();
            int r = (int) current[2];
            int c = (int) current[3];
            if (current[1] > bestCost[r][c]) {
                continue;
            }
            if (r == goal[0] && c == goal[1]) {
                return reconstruct(jumpFrom, goal);
            }
            for (int[] direction : DIRECTIONS) {
                int jump = jumpInDirection(walkable, r, c, direction[0],
                        direction[1], goal);
                if (jump == -1) {
                    continue;
                }
                int jr = jump / columns;
                int jc = jump % columns;
                double cost = current[1] + octile(r, c, new int[]{jr, jc});
                if (cost < bestCost[jr][jc]) {
                    bestCost[jr][jc] = cost;
                    jumpFrom[jr][jc] = r * columns + c;
                    open.add(new double[]{cost + octile(jr, jc, goal), cost, jr, jc});
                }
            }
        }
        return List.of();
    }

    /**
     * (r,c) 沿方向寻下一跳点：直扫遇强制邻格/终点即返；斜扫任一子直扫命中
     * 即返。
     */
    private static int jumpInDirection(boolean[][] walkable, int row, int column,
            int dr, int dc, int[] goal) {
        int columns = walkable[0].length;
        int r = row;
        int c = column;
        while (true) {
            int nr = r + dr;
            int nc = c + dc;
            if (!inGrid(walkable.length, columns, nr, nc) || !walkable[nr][nc]) {
                return -1;
            }
            r = nr;
            c = nc;
            if (r == goal[0] && c == goal[1]) {
                return r * columns + c;
            }
            if (dr != 0 && dc != 0) {
                if (jumpInLine(walkable, r, c, dr, 0, goal) != -1
                        || jumpInLine(walkable, r, c, 0, dc, goal) != -1) {
                    return r * columns + c;
                }
            } else if (hasForcedNeighbor(walkable, r, c, dr, dc)) {
                return r * columns + c;
            }
        }
    }

    /** 直线子扫：遇终点或强制邻格返回格编码，否则 −1。 */
    private static int jumpInLine(boolean[][] walkable, int row, int column,
            int dr, int dc, int[] goal) {
        int columns = walkable[0].length;
        int r = row;
        int c = column;
        while (true) {
            int nr = r + dr;
            int nc = c + dc;
            if (!inGrid(walkable.length, columns, nr, nc) || !walkable[nr][nc]) {
                return -1;
            }
            r = nr;
            c = nc;
            if (r == goal[0] && c == goal[1]) {
                return r * columns + c;
            }
            if (hasForcedNeighbor(walkable, r, c, dr, dc)) {
                return r * columns + c;
            }
        }
    }

    /** 强制邻格：直向前进受阻侧向开口（原 paper 判据）。 */
    private static boolean hasForcedNeighbor(boolean[][] walkable, int r, int c,
            int dr, int dc) {
        int rows = walkable.length;
        int columns = walkable[0].length;
        if (dr != 0) {
            return (inGrid(rows, columns, r, c + 1) && !walkable[r][c + 1]
                    && inGrid(rows, columns, r + dr, c + 1) && walkable[r + dr][c + 1])
                    || (inGrid(rows, columns, r, c - 1) && !walkable[r][c - 1]
                    && inGrid(rows, columns, r + dr, c - 1) && walkable[r + dr][c - 1]);
        }
        return (inGrid(rows, columns, r + 1, c) && !walkable[r + 1][c]
                && inGrid(rows, columns, r + 1, c + dc) && walkable[r + 1][c + dc])
                || (inGrid(rows, columns, r - 1, c) && !walkable[r - 1][c]
                && inGrid(rows, columns, r - 1, c + dc) && walkable[r - 1][c + dc]);
    }

    private static boolean inGrid(int rows, int columns, int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < columns;
    }

    private static double octile(int r, int c, int[] goal) {
        int dr = Math.abs(r - goal[0]);
        int dc = Math.abs(c - goal[1]);
        return dr + dc + (Math.sqrt(2.0) - 2.0) * Math.min(dr, dc);
    }

    /** 跳点链回溯逐格展开全路径。 */
    private static List<int[]> reconstruct(int[][] jumpFrom, int[] goal) {
        int columns = jumpFrom[0].length;
        List<Integer> chain = new ArrayList<>();
        int node = goal[0] * columns + goal[1];
        while (node != -1) {
            chain.add(node);
            node = jumpFrom[node / columns][node % columns];
        }
        java.util.Collections.reverse(chain);
        List<int[]> path = new ArrayList<>();
        for (int i = 0; i < chain.size(); i++) {
            int current = chain.get(i);
            path.add(new int[]{current / columns, current % columns});
            if (i + 1 < chain.size()) {
                expandSegment(path, current, chain.get(i + 1), columns);
            }
        }
        return path;
    }

    /** 相邻跳点间逐格插值（对角步+直向步确定展开；终点格由主循环补）。 */
    private static void expandSegment(List<int[]> path, int from, int to,
            int columns) {
        int r = from / columns;
        int c = from % columns;
        int tr = to / columns;
        int tc = to % columns;
        while (r != tr || c != tc) {
            int dr = Integer.compare(tr, r);
            int dc = Integer.compare(tc, c);
            if (dr != 0) {
                r += dr;
            }
            if (dc != 0) {
                c += dc;
            }
            if (r != tr || c != tc) {
                path.add(new int[]{r, c});
            }
        }
    }

    private static void validate(boolean[][] walkable, int[] start, int[] goal) {
        if (walkable == null || start == null || goal == null) {
            throw new IllegalArgumentException("网格/起终点非 null");
        }
        if (walkable.length == 0 || walkable[0].length == 0) {
            throw new IllegalArgumentException("网格非空");
        }
        requireInGrid(walkable, start, "起点");
        requireInGrid(walkable, goal, "终点");
        if (!walkable[start[0]][start[1]]) {
            throw new IllegalArgumentException("起点不可走");
        }
        if (!walkable[goal[0]][goal[1]]) {
            throw new IllegalArgumentException("终点不可走");
        }
    }

    private static void requireInGrid(boolean[][] walkable, int[] point, String name) {
        if (point.length != 2 || point[0] < 0 || point[0] >= walkable.length
                || point[1] < 0 || point[1] >= walkable[0].length) {
            throw new IllegalArgumentException(name + "越界（rows=" + walkable.length
                    + " cols=" + walkable[0].length + "）");
        }
    }
}
