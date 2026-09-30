package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Arrays;

/**
 * 几何中位数（spec 9033 / W9067 / impl 2386）——Weiszfeld 1937
 * 思想（设施选址/Fermat–Weber 点同源——k-means 前身与 GIS 选址
 * 同源）：**y ← Σ(p_i/‖p_i−y‖)/Σ(1/‖p_i−y‖) 加权重心迭代——
 * 到全部点欧氏距离和最小的点**（无解析解的经典凸问题）——
 * 质心最小化平方距离（对离群点敏感）与中位数坐标轴独立
 * （非联合最优）的中间形态：L1 联合最优。迭代次数入参（确定
 * 收敛步数——非容忍度承诺，诚实边界）；零距离防除（ε 抬升）；
 * 确定性纯函数；null/空/维度不一致 fail-fast。
 *
 * <p>与 KdTree/ClosestPair（同包）同域不同面：近邻查询 vs
 * 全局选址；与 KMeansClustering（eval 域）同根：单中心 vs
 * 多中心划分。
 */
public final class GeometricMedian {

    private static final double EPSILON = 1e-12;

    private GeometricMedian() {
    }

    /**
     * 几何中位数（Weiszfeld 迭代——返回到全点距离和最小的近似点）。
     *
     * @throws IllegalArgumentException null/空点集/维度不一致
     */
    public static double[] median(double[][] points, int iterations) {
        if (points == null || points.length == 0) {
            throw new IllegalArgumentException("点集非空（≥1 点）");
        }
        if (iterations < 1) {
            throw new IllegalArgumentException("迭代次数为正（实际 " + iterations + "）");
        }
        int dimension = points[0].length;
        if (dimension == 0) {
            throw new IllegalArgumentException("维度 ≥1");
        }
        for (double[] point : points) {
            if (point == null || point.length != dimension) {
                throw new IllegalArgumentException("维度一致（首点 " + dimension + "）");
            }
        }
        if (points.length == 1) {
            return Arrays.copyOf(points[0], dimension);
        }
        // 质心起步（凸性下单调收敛）
        double[] y = new double[dimension];
        for (double[] point : points) {
            for (int d = 0; d < dimension; d++) {
                y[d] += point[d];
            }
        }
        for (int d = 0; d < dimension; d++) {
            y[d] /= points.length;
        }
        for (int it = 0; it < iterations; it++) {
            double[] next = new double[dimension];
            double weightSum = 0;
            for (double[] point : points) {
                double distance = distance(point, y);
                double weight = 1.0 / Math.max(distance, EPSILON);
                weightSum += weight;
                for (int d = 0; d < dimension; d++) {
                    next[d] += point[d] * weight;
                }
            }
            for (int d = 0; d < dimension; d++) {
                y[d] = next[d] / weightSum;
            }
        }
        return y;
    }

    /** 到全点欧氏距离和（目标函数读数——迭代改进性校验面）。 */
    public static double totalDistance(double[][] points, double[] candidate) {
        double total = 0;
        for (double[] point : points) {
            total += distance(point, candidate);
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
}
