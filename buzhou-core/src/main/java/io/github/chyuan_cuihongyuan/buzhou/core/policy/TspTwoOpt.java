package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Arrays;

/**
 * TSP 2-opt 局部搜索（spec 9045 / W9091 / impl 2398）——Croes 1958
 * 思想（2-opt 边交换——路由规划/物流配送/芯片布线同源）：
 * **断两条边重接（区间反转）消交叉——凸包性质保证 Euclidean
 * TSP 最优解无交叉边，2-opt 单调去交叉逼近**——初始序直行
 * （自交路径长度放大）与全排列枚举 O(n!) 的病解。首达改进
 * 策略（first-improvement，i<j 双扫——确定取序）；maxRounds
 * 上限无改进即收敛停；返回闭合环游（含返程）与总长；
 * null/点数<2/轮数<0 fail-fast；确定性纯函数。
 *
 * <p>与 SimulatedAnnealing（spec 9044）衔接：组合结构邻域 vs
 * 连续点扰动；与 GeometricMedian（同包）同域不同面：单点
 * 选址 vs 环序优化。
 */
public final class TspTwoOpt {

    /** 优化结果（order 环游序 0..n−1；length 闭合总长含返程）。 */
    public record Tour(int[] order, double length) {
    }

    private TspTwoOpt() {
    }

    /**
     * 2-opt 局部优化（初始环游 = 输入序 0..n−1）。
     *
     * @throws IllegalArgumentException null/点数<2/轮数<0/维度不一致
     */
    public static Tour optimize(double[][] points, int maxRounds) {
        if (points == null || points.length < 2) {
            throw new IllegalArgumentException("点集 ≥2（环游语义）");
        }
        if (maxRounds < 0) {
            throw new IllegalArgumentException("轮数非负（实际 " + maxRounds + "）");
        }
        int n = points.length;
        int dim = points[0].length;
        for (double[] point : points) {
            if (point == null || point.length != dim) {
                throw new IllegalArgumentException("维度一致（首点 " + dim + "）");
            }
        }
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        double length = tourLength(points, order);
        for (int round = 0; round <= maxRounds; round++) {
            boolean improved = false;
            for (int i = 0; i < n - 1 && !improved; i++) {
                for (int j = i + 2; j < n; j++) {
                    if (i == 0 && j == n - 1) {
                        continue; // 相邻边（闭环同边）免换
                    }
                    int a = order[i];
                    int b = order[i + 1];
                    int c = order[j];
                    int d = order[(j + 1) % n];
                    double delta = distance(points[a], points[c]) + distance(points[b], points[d])
                            - distance(points[a], points[b]) - distance(points[c], points[d]);
                    if (delta < -1e-12) {
                        reverse(order, i + 1, j);
                        length += delta;
                        improved = true;
                        break;
                    }
                }
            }
            if (!improved) {
                break;
            }
        }
        return new Tour(order, length);
    }

    private static void reverse(int[] order, int from, int to) {
        while (from < to) {
            int tmp = order[from];
            order[from] = order[to];
            order[to] = tmp;
            from++;
            to--;
        }
    }

    private static double tourLength(double[][] points, int[] order) {
        double total = 0;
        for (int i = 0; i < order.length; i++) {
            total += distance(points[order[i]], points[order[(i + 1) % order.length]]);
        }
        return total;
    }

    private static double distance(double[] a, double[] b) {
        double squares = 0;
        for (int d = 0; d < a.length; d++) {
            double diff = a[d] - b[d];
            squares += diff * diff;
        }
        return Math.sqrt(squares);
    }

    /** 独立环长计算（校验面）。 */
    public static double lengthOf(double[][] points, int[] order) {
        if (order == null || order.length != points.length) {
            throw new IllegalArgumentException("环序与点数一致");
        }
        return tourLength(points, order);
    }
}
