package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.GaussianElimination;

/**
 * OLS 正规方程线性回归（spec 10044 / X10089 / impl 2447）——Gauss–Markov/
 * NumPy lstsq 思想（scikit-learn LinearRegression 同源）：**设计阵内联截距列
 * （n×(d+1)）+ 正规方程 (AᵀA)w=Aᵀy 闭式解**——解线性系统消费
 * GaussianElimination（spec 10006，core/concurrent）——流程间自组合。
 *
 * <p>条件数病态域不在承诺（QR/SVD 稳定解法另立）；奇异矩阵上浮 fail-fast。
 * null/锯齿行/长度不配/空集 fail-fast；同输入复算确定。
 */
public final class LinearRegression {

    private LinearRegression() {
    }

    /**
     * 最小二乘拟合。
     *
     * @param X 特征行（等长，无截距列）
     * @param y 目标（与 X 行数一致）
     * @return 系数 [截距, β1..βd]
     * @throws IllegalArgumentException null/锯齿行/长度不配/空集
     * @throws IllegalArgumentException 奇异正规矩阵（上浮自求解器）
     */
    public static double[] fit(double[][] X, double[] y) {
        validate(X, y);
        int rows = X.length;
        int columns = X[0].length + 1;
        double[][] design = new double[rows][columns];
        for (int i = 0; i < rows; i++) {
            design[i][0] = 1.0;
            System.arraycopy(X[i], 0, design[i], 1, columns - 1);
        }
        double[][] normal = new double[columns][columns];
        double[] rhs = new double[columns];
        for (int i = 0; i < columns; i++) {
            for (int j = 0; j < columns; j++) {
                double sum = 0.0;
                for (int r = 0; r < rows; r++) {
                    sum += design[r][i] * design[r][j];
                }
                normal[i][j] = sum;
            }
            double sum = 0.0;
            for (int r = 0; r < rows; r++) {
                sum += design[r][i] * y[r];
            }
            rhs[i] = sum;
        }
        return GaussianElimination.solve(normal, rhs);
    }

    private static void validate(double[][] X, double[] y) {
        if (X == null || y == null) {
            throw new IllegalArgumentException("特征/目标非 null");
        }
        if (X.length == 0) {
            throw new IllegalArgumentException("样本集非空");
        }
        if (X.length != y.length) {
            throw new IllegalArgumentException("长度不配（X=" + X.length
                    + " y=" + y.length + "）");
        }
        int columns = X[0].length;
        for (int i = 0; i < X.length; i++) {
            if (X[i] == null || X[i].length != columns) {
                throw new IllegalArgumentException("等长特征行（第 " + i + " 行）");
            }
            for (double v : X[i]) {
                if (!Double.isFinite(v)) {
                    throw new IllegalArgumentException("特征非有限（第 " + i + " 行）");
                }
            }
            if (!Double.isFinite(y[i])) {
                throw new IllegalArgumentException("目标非有限（第 " + i + " 行）");
            }
        }
    }
}
