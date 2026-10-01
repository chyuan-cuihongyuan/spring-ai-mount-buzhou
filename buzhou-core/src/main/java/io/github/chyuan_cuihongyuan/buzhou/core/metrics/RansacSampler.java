package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Random;

/**
 * RANSAC 直线拟合（spec 11014 / Y11029 / impl 2467）——Fischler–Bolles 1981
 * 思想（OpenCV 同源）：**随机二点采样定线 + 阈值共识内点计数最大胜出**——
 * 污染观测的鲁棒拟合面（LinearRegression 闭式最小二乘的外点病解互补）。
 * 退化样本（x 差零）跳过；Line 嵌套 record。
 *
 * <p>iterations≥1/threshold≥0/点数≥2；null/锯齿行/非有限值 fail-fast；
 * 同种子复算确定。
 */
public final class RansacSampler {

    private RansacSampler() {
    }

    /** 拟合线（斜率+截距）与共识规模。 */
    public record Line(double slope, double intercept, int inliers) {
    }

    /**
     * RANSAC 线拟合。
     *
     * @param points 二维点行 {x,y}（≥2）
     * @param iterations 采样轮数（≥1）
     * @param threshold 内点距离阈值（≥0）
     * @param seed 随机种子
     * @throws IllegalArgumentException null/点数不足/非法超参/非有限值
     */
    public static Line fitLine(double[][] points, int iterations, double threshold,
            long seed) {
        validate(points, iterations, threshold);
        Random random = new Random(seed);
        Line best = null;
        for (int iteration = 0; iteration < iterations; iteration++) {
            int first = random.nextInt(points.length);
            int second = random.nextInt(points.length);
            if (first == second) {
                continue;
            }
            double dx = points[second][0] - points[first][0];
            if (dx == 0.0) {
                continue;
            }
            double slope = (points[second][1] - points[first][1]) / dx;
            double intercept = points[first][1] - slope * points[first][0];
            int inliers = 0;
            for (double[] point : points) {
                if (Math.abs(point[1] - (slope * point[0] + intercept)) < threshold) {
                    inliers++;
                }
            }
            if (best == null || inliers > best.inliers()) {
                best = new Line(slope, intercept, inliers);
            }
        }
        if (best == null) {
            throw new IllegalArgumentException("无有效样本对（全退化或点数不足）");
        }
        return best;
    }

    private static void validate(double[][] points, int iterations, double threshold) {
        if (points == null || points.length < 2) {
            throw new IllegalArgumentException("点数 ≥2（实际 "
                    + (points == null ? "null" : points.length) + "）");
        }
        if (iterations < 1) {
            throw new IllegalArgumentException("迭代数为正（实际 " + iterations + "）");
        }
        if (!(threshold >= 0.0) || !Double.isFinite(threshold)) {
            throw new IllegalArgumentException("阈值非负有限（实际 " + threshold + "）");
        }
        for (int i = 0; i < points.length; i++) {
            if (points[i] == null || points[i].length != 2) {
                throw new IllegalArgumentException("点须为 {x,y}（第 " + i + " 行）");
            }
            for (double v : points[i]) {
                if (!Double.isFinite(v)) {
                    throw new IllegalArgumentException("坐标非有限（第 " + i + " 行）");
                }
            }
        }
    }
}
