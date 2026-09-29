package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.Arrays;

/**
 * 幂迭代（spec 8032 / V8065 / impl 2334）——
 * Page & Brin 1998 PageRank 幂迭代思想：**反复
 * x←Ax/‖Ax‖，谱半径占优的主特征分量指数收敛**——特征
 * 分解 O(n³)（仅要主分量浪费）的病解。Rayleigh 商
 * xᵀAx/xᵀx 读数；maxIter/容差双停；非方阵/维度不一致/
 * 负容差 fail-fast；确定性纯函数。
 *
 * <p>与 KMeansClustering（spec 8030）同族不同面：谱结构
 * vs 划分结构。
 */
public final class PowerIteration {

    private PowerIteration() {
    }

    /** 主特征向量与 Rayleigh 商特征值（结果[0..n-1]=向量、[n]=特征值）。 */
    public static double[] dominant(double[][] matrix, int maxIterations, double tolerance) {
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException("矩阵非空");
        }
        int n = matrix.length;
        for (double[] row : matrix) {
            if (row == null || row.length != n) {
                throw new IllegalArgumentException("方阵语义（行宽 " + (row == null ? -1 : row.length)
                        + " vs " + n + "）");
            }
        }
        if (maxIterations < 1) {
            throw new IllegalArgumentException("maxIter 非负（实际 " + maxIterations + "）");
        }
        if (tolerance < 0) {
            throw new IllegalArgumentException("容差非负（实际 " + tolerance + "）");
        }
        double[] vector = new double[n];
        Arrays.fill(vector, 1.0 / Math.sqrt(n));
        double eigenvalue = rayleighQuotient(matrix, vector);
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            double[] next = multiply(matrix, vector);
            double norm = euclideanNorm(next);
            if (norm == 0) {
                throw new IllegalArgumentException("零向量迭代（零矩阵——主分量无定义）");
            }
            for (int i = 0; i < n; i++) {
                next[i] /= norm;
            }
            double nextEigenvalue = rayleighQuotient(matrix, next);
            double delta = Math.abs(nextEigenvalue - eigenvalue);
            vector = next;
            eigenvalue = nextEigenvalue;
            if (delta <= tolerance) {
                break;
            }
        }
        double[] result = Arrays.copyOf(vector, n + 1);
        result[n] = eigenvalue;
        return result;
    }

    /** Rayleigh 商 xᵀAx/xᵀx。 */
    public static double rayleighQuotient(double[][] matrix, double[] vector) {
        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < vector.length; i++) {
            double rowSum = 0;
            for (int j = 0; j < vector.length; j++) {
                rowSum += matrix[i][j] * vector[j];
            }
            numerator += vector[i] * rowSum;
            denominator += vector[i] * vector[i];
        }
        return numerator / denominator;
    }

    private static double[] multiply(double[][] matrix, double[] vector) {
        double[] out = new double[vector.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < vector.length; j++) {
                out[i] += matrix[i][j] * vector[j];
            }
        }
        return out;
    }

    private static double euclideanNorm(double[] vector) {
        double sum = 0;
        for (double value : vector) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }
}
