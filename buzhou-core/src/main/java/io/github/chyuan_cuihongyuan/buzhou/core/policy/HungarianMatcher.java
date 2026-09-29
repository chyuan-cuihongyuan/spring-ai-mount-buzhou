package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Arrays;

/**
 * 匈牙利指派匹配（spec 8007 / V8015 / impl 2309）——
 * Kuhn 1955 / Munkres 1957 思想（任务指派/推荐配对同源）：
 * **对偶位势 u/v 调整 + 交替树增广**把总代价压到全局最小
 * O(n³)——全排列枚举 O(n!)（规模放大）与贪心逐行取最小
 * （局部最优锁死全局，后行被迫付高价）两种病的同解。
 * 方阵语义；minCost/assignment 双面；并列代价取算法推进序
 * （确定性——同阵同指派）；非方阵/null/空阵 fail-fast。
 *
 * <p>与 ActivitySelectionGreedy（spec 7045）同族不同面：
 * 区间兼容贪心 vs 指派全局最优对偶调整。
 */
public final class HungarianMatcher {

    private static final long INFINITY = Long.MAX_VALUE / 4;

    private HungarianMatcher() {
    }

    /** 最小总代价（方阵）。 */
    public static long minCost(long[][] cost) {
        int[] assignment = assignment(cost);
        long total = 0;
        for (int row = 0; row < assignment.length; row++) {
            total += cost[row][assignment[row]];
        }
        return total;
    }

    /** 行→列指派（返回长度 n 的数组，assignment[row]=col——行列双射）。 */
    public static int[] assignment(long[][] cost) {
        if (cost == null || cost.length == 0) {
            throw new IllegalArgumentException("代价方阵非空");
        }
        int n = cost.length;
        for (long[] row : cost) {
            if (row == null || row.length != n) {
                throw new IllegalArgumentException("方阵语义（行数 " + n + "，存在 "
                        + (row == null ? "null" : row.length) + " 列行）");
            }
        }
        long[] u = new long[n + 1];
        long[] v = new long[n + 1];
        int[] pot = new int[n + 1];
        int[] way = new int[n + 1];
        for (int i = 1; i <= n; i++) {
            pot[0] = i;
            int j0 = 0;
            long[] minv = new long[n + 1];
            boolean[] used = new boolean[n + 1];
            Arrays.fill(minv, INFINITY);
            do {
                used[j0] = true;
                int i0 = pot[j0];
                long delta = INFINITY;
                int j1 = -1;
                for (int j = 1; j <= n; j++) {
                    if (used[j]) {
                        continue;
                    }
                    long cur = cost[i0 - 1][j - 1] - u[i0] - v[j];
                    if (cur < minv[j]) {
                        minv[j] = cur;
                        way[j] = j0;
                    }
                    if (minv[j] < delta) {
                        delta = minv[j];
                        j1 = j;
                    }
                }
                for (int j = 0; j <= n; j++) {
                    if (used[j]) {
                        u[pot[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }
                j0 = j1;
            } while (pot[j0] != 0);
            do {
                int j1 = way[j0];
                pot[j0] = pot[j1];
                j0 = j1;
            } while (j0 != 0);
        }
        int[] rowToCol = new int[n];
        for (int j = 1; j <= n; j++) {
            if (pot[j] > 0) {
                rowToCol[pot[j] - 1] = j - 1;
            }
        }
        return rowToCol;
    }
}
