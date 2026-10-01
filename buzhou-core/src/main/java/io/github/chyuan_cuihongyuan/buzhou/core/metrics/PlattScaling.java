package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.GaussianElimination;

/**
 * Platt 校准（spec 11016 / Y11033 / impl 2469）——Platt 1999 思想（LIBSVM/
 * sklearn 同源）：**Newton 迭代最小化交叉熵拟合 p=sigmoid(A·s+B)**——SVM
 * 分数到概率的参数化校准面（IsotonicCalibration 的非参数互补面）。阻尼
 * Hessian 高斯消元步进（解线性系统消费 GaussianElimination——库内互喂
 * 第三例）。
 *
 * <p>标签 {0,1}/迭代数 ≥1/分数有限 fail-fast；同输入复算确定。
 */
public final class PlattScaling {

    /** Hessian 阻尼（数值稳定项）。 */
    private static final double DAMPING = 1e-6;

    private final double slope;
    private final double intercept;

    private PlattScaling(double slope, double intercept) {
        this.slope = slope;
        this.intercept = intercept;
    }

    /** 校准参数（A 斜率 + B 截距）。 */
    public record Parameters(double a, double b) {
    }

    /**
     * 拟合校准参数。
     *
     * @param scores 分数
     * @param labels 标签 {0,1}
     * @param iterations Newton 迭代数（≥1）
     * @throws IllegalArgumentException null/长度不配/标签越界/非法迭代数
     */
    public static PlattScaling fit(double[] scores, int[] labels, int iterations) {
        validate(scores, labels, iterations);
        double a = 0.0;
        double b = Math.log((countPositives(labels) + 1.0)
                / (labels.length - countPositives(labels) + 1.0));
        for (int iteration = 0; iteration < iterations; iteration++) {
            double h00 = DAMPING;
            double h01 = 0.0;
            double h11 = labels.length * 1.0 + DAMPING;
            double g0 = 0.0;
            double g1 = 0.0;
            for (int i = 0; i < scores.length; i++) {
                double p = sigmoid(a * scores[i] + b);
                double w = p * (1.0 - p);
                double residual = p - labels[i];
                h00 += w * scores[i] * scores[i];
                h01 += w * scores[i];
                g0 += residual * scores[i];
                g1 += residual;
            }
            double[] step = GaussianElimination.solve(
                    new double[][]{{h00, h01}, {h01, h11}},
                    new double[]{g0, g1});
            a -= step[0];
            b -= step[1];
        }
        return new PlattScaling(a, b);
    }

    /**
     * 校准概率。
     *
     * @throws IllegalArgumentException 分数非有限
     */
    public double probability(double score) {
        if (!Double.isFinite(score)) {
            throw new IllegalArgumentException("分数非有限（实际 " + score + "）");
        }
        return sigmoid(slope * score + intercept);
    }

    /** 参数读面。 */
    public Parameters parameters() {
        return new Parameters(slope, intercept);
    }

    private static double sigmoid(double z) {
        if (z >= 0) {
            return 1.0 / (1.0 + Math.exp(-z));
        }
        double e = Math.exp(z);
        return e / (1.0 + e);
    }

    private static int countPositives(int[] labels) {
        int count = 0;
        for (int label : labels) {
            if (label == 1) {
                count++;
            }
        }
        return count;
    }

    private static void validate(double[] scores, int[] labels, int iterations) {
        if (scores == null || labels == null) {
            throw new IllegalArgumentException("分数/标签非 null");
        }
        if (scores.length != labels.length) {
            throw new IllegalArgumentException("长度不配（s=" + scores.length
                    + " y=" + labels.length + "）");
        }
        if (iterations < 1) {
            throw new IllegalArgumentException("迭代数为正（实际 " + iterations + "）");
        }
        for (int i = 0; i < scores.length; i++) {
            if (!Double.isFinite(scores[i])) {
                throw new IllegalArgumentException("分数非有限（第 " + i + " 位）");
            }
            if (labels[i] != 0 && labels[i] != 1) {
                throw new IllegalArgumentException("标签取 {0,1}（第 " + i + " 位实际 "
                        + labels[i] + "）");
            }
        }
    }
}
