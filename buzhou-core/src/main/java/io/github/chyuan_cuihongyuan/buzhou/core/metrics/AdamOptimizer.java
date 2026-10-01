package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Adam 自适应矩估计（spec 10045 / X10091 / impl 2448）——Kingma–Ba 2015 思想
 * （PyTorch/TensorFlow 同源）：**一阶矩 m=β1m+(1−β1)g、二阶矩 v=β2v+(1−β2)g²
 * 指数滑动+bias 修正（m̂=m/(1−β1^t)、v̂=v/(1−β2^t)）+
 * w−=lr·m̂/(√v̂+ε)**——纯函数无状态面（终值向量返回，梯度全量由 Objective 供）。
 *
 * <p>超参域：lr>0、β∈[0,1)、ε>0、iters≥1；null/非有限/维数零 fail-fast；
 * 同输入复算确定。
 */
public final class AdamOptimizer {

    private AdamOptimizer() {
    }

    /** 目标函数（值+梯度）。 */
    public interface Objective {

        /** 目标值。 */
        double valueAt(double[] weights);

        /** 梯度。 */
        double[] gradientAt(double[] weights);
    }

    /**
     * Adam 最小化。
     *
     * @param objective 目标函数
     * @param initial 初始点
     * @param learningRate 步长（>0）
     * @param beta1 一阶矩衰减（[0,1)）
     * @param beta2 二阶矩衰减（[0,1)）
     * @param epsilon 数值稳定项（>0）
     * @param iterations 迭代数（≥1）
     * @throws IllegalArgumentException null/非有限/超参越域
     */
    public static double[] minimize(Objective objective, double[] initial,
            double learningRate, double beta1, double beta2, double epsilon,
            int iterations) {
        validate(objective, initial, learningRate, beta1, beta2, epsilon, iterations);
        int n = initial.length;
        double[] weights = initial.clone();
        double[] firstMoment = new double[n];
        double[] secondMoment = new double[n];
        for (int t = 1; t <= iterations; t++) {
            double[] gradient = objective.gradientAt(weights);
            double biasCorrection1 = 1.0 - Math.pow(beta1, t);
            double biasCorrection2 = 1.0 - Math.pow(beta2, t);
            for (int i = 0; i < n; i++) {
                firstMoment[i] = beta1 * firstMoment[i]
                        + (1.0 - beta1) * gradient[i];
                secondMoment[i] = beta2 * secondMoment[i]
                        + (1.0 - beta2) * gradient[i] * gradient[i];
                double correctedFirst = firstMoment[i] / biasCorrection1;
                double correctedSecond = secondMoment[i] / biasCorrection2;
                weights[i] -= learningRate * correctedFirst
                        / (Math.sqrt(correctedSecond) + epsilon);
            }
        }
        return weights;
    }

    private static void validate(Objective objective, double[] initial,
            double learningRate, double beta1, double beta2, double epsilon,
            int iterations) {
        if (objective == null || initial == null) {
            throw new IllegalArgumentException("目标/初始点非 null");
        }
        if (initial.length == 0) {
            throw new IllegalArgumentException("维数为正");
        }
        if (!(learningRate > 0.0) || !Double.isFinite(learningRate)) {
            throw new IllegalArgumentException("步长正有限（实际 " + learningRate + "）");
        }
        if (!(beta1 >= 0.0 && beta1 < 1.0) || !(beta2 >= 0.0 && beta2 < 1.0)) {
            throw new IllegalArgumentException("β∈[0,1)（实际 " + beta1 + "," + beta2 + "）");
        }
        if (!(epsilon > 0.0) || !Double.isFinite(epsilon)) {
            throw new IllegalArgumentException("ε 正有限（实际 " + epsilon + "）");
        }
        if (iterations < 1) {
            throw new IllegalArgumentException("迭代数为正（实际 " + iterations + "）");
        }
        for (double w : initial) {
            if (!Double.isFinite(w)) {
                throw new IllegalArgumentException("初始点非有限");
            }
        }
    }
}
