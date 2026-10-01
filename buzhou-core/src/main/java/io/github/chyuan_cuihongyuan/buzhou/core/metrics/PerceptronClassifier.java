package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 感知机（spec 10043 / X10087 / impl 2446）——Rosenblatt 1958 思想
 * （scikit-learn Perceptron 同源）：**错分驱动的在线超平面更新
 * w += lr·(y−ŷ)·x、b += lr·(y−ŷ)**——线性二分类最简判别面（标签 {0,1}；
 * 线性可分域承诺——不可分不收敛明示）。样本序固定遍历（确定性口径）。
 *
 * <p>null/锯齿行/标签越界/非正学习率/非正轮数/空集 fail-fast；模型不可变。
 */
public final class PerceptronClassifier {

    private final double[] weights;
    private final double bias;

    private PerceptronClassifier(double[] weights, double bias) {
        this.weights = weights;
        this.bias = bias;
    }

    /**
     * 拟合。
     *
     * @param features 特征行（等长）
     * @param labels 标签 {0,1}
     * @param learningRate 学习率（>0）
     * @param epochs 轮数（≥1）
     * @throws IllegalArgumentException null/锯齿行/标签越界/非法超参/空集
     */
    public static PerceptronClassifier fit(double[][] features, int[] labels,
            double learningRate, int epochs) {
        validate(features, labels, learningRate, epochs);
        int dimension = features[0].length;
        double[] weights = new double[dimension];
        double bias = 0.0;
        for (int epoch = 0; epoch < epochs; epoch++) {
            for (int i = 0; i < features.length; i++) {
                double activation = bias;
                for (int j = 0; j < dimension; j++) {
                    activation += weights[j] * features[i][j];
                }
                int predicted = activation >= 0.0 ? 1 : 0;
                double error = labels[i] - predicted;
                if (error != 0.0) {
                    for (int j = 0; j < dimension; j++) {
                        weights[j] += learningRate * error * features[i][j];
                    }
                    bias += learningRate * error;
                }
            }
        }
        return new PerceptronClassifier(weights, bias);
    }

    /**
     * 预测（激活值符号面）。
     *
     * @throws IllegalArgumentException null/维数不配
     */
    public int predict(double[] features) {
        if (features == null) {
            throw new IllegalArgumentException("特征非 null");
        }
        if (features.length != weights.length) {
            throw new IllegalArgumentException("维数不配（x=" + features.length
                    + " w=" + weights.length + "）");
        }
        double activation = bias;
        for (int j = 0; j < weights.length; j++) {
            activation += weights[j] * features[j];
        }
        return activation >= 0.0 ? 1 : 0;
    }

    private static void validate(double[][] features, int[] labels,
            double learningRate, int epochs) {
        if (features == null || labels == null) {
            throw new IllegalArgumentException("样本/标签非 null");
        }
        if (features.length == 0) {
            throw new IllegalArgumentException("样本集非空");
        }
        if (features.length != labels.length) {
            throw new IllegalArgumentException("长度不配（x=" + features.length
                    + " y=" + labels.length + "）");
        }
        if (!(learningRate > 0.0)) {
            throw new IllegalArgumentException("学习率为正（实际 " + learningRate + "）");
        }
        if (epochs < 1) {
            throw new IllegalArgumentException("轮数为正（实际 " + epochs + "）");
        }
        int dimension = features[0].length;
        if (dimension == 0) {
            throw new IllegalArgumentException("特征维数为正");
        }
        for (int i = 0; i < features.length; i++) {
            if (features[i] == null || features[i].length != dimension) {
                throw new IllegalArgumentException("等长特征行（第 " + i + " 行）");
            }
            if (labels[i] != 0 && labels[i] != 1) {
                throw new IllegalArgumentException("标签取 {0,1}（第 " + i + " 行实际 "
                        + labels[i] + "）");
            }
            for (double v : features[i]) {
                if (!Double.isFinite(v)) {
                    throw new IllegalArgumentException("特征非有限（第 " + i + " 行）");
                }
            }
        }
    }
}
