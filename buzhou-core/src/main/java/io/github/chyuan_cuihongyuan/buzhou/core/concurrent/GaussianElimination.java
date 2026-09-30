package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 高斯消元（spec 10006 / X10013 / impl 2409）——Gauss 1810 思想
 * （「部分主元前向消元+回代」——NumPy linalg.solve/JAMA/LAPACK
 * dgesv 同源）：**逐列选绝对值最大主元行交换（数值稳定的关键——
 * 裸序消元会让小主元放大舍入误差），前向消成上三角再回代**——
 * 线性方程组 Ax=b 的基准直接法。方阵契约；奇异（主元 < 容差）
 * fail-fast 而非静默返回；非方阵/维数不配/null fail-fast；确定性
 * （同输入同解——无随机性）。
 */
public final class GaussianElimination {

    /** 奇异性主元容差（绝对值低于此视为奇异——诚实边界入档）。 */
    private static final double SINGULARITY_TOLERANCE = 1e-12;

    private GaussianElimination() {
    }

    /**
     * 解 Ax=b（部分主元高斯消元）。
     *
     * @throws IllegalArgumentException null/非方阵/维数不配/奇异
     */
    public static double[] solve(double[][] a, double[] b) {
        validate(a, b);
        int n = b.length;
        double[][] m = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, m[i], 0, n);
            m[i][n] = b[i];
        }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) {
                    pivot = row;
                }
            }
            if (Math.abs(m[pivot][col]) < SINGULARITY_TOLERANCE) {
                throw new IllegalArgumentException("矩阵奇异（列 " + col
                        + " 主元绝对值 " + Math.abs(m[pivot][col]) + "）");
            }
            double[] tmp = m[col];
            m[col] = m[pivot];
            m[pivot] = tmp;
            for (int row = col + 1; row < n; row++) {
                double factor = m[row][col] / m[col][col];
                for (int j = col; j <= n; j++) {
                    m[row][j] -= factor * m[col][j];
                }
            }
        }
        double[] x = new double[n];
        for (int row = n - 1; row >= 0; row--) {
            double sum = m[row][n];
            for (int j = row + 1; j < n; j++) {
                sum -= m[row][j] * x[j];
            }
            x[row] = sum / m[row][row];
        }
        return x;
    }

    /**
     * 行列式（同款部分主元消元——交换计负号）。
     *
     * @throws IllegalArgumentException null/非方阵/奇异容差内返 0 契约
     */
    public static double determinant(double[][] a) {
        validateSquare(a);
        int n = a.length;
        double[][] m = new double[n][];
        for (int i = 0; i < n; i++) {
            m[i] = a[i].clone();
        }
        double det = 1.0;
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) {
                    pivot = row;
                }
            }
            if (Math.abs(m[pivot][col]) < SINGULARITY_TOLERANCE) {
                return 0.0;
            }
            if (pivot != col) {
                double[] tmp = m[col];
                m[col] = m[pivot];
                m[pivot] = tmp;
                det = -det;
            }
            det *= m[col][col];
            for (int row = col + 1; row < n; row++) {
                double factor = m[row][col] / m[col][col];
                for (int j = col; j < n; j++) {
                    m[row][j] -= factor * m[col][j];
                }
            }
        }
        return det;
    }

    private static void validate(double[][] a, double[] b) {
        validateSquare(a);
        if (b == null || b.length != a.length) {
            throw new IllegalArgumentException("右端维数与方阵阶数相配（实际 "
                    + (b == null ? "null" : b.length) + " vs " + a.length + "）");
        }
    }

    private static void validateSquare(double[][] a) {
        if (a == null || a.length == 0) {
            throw new IllegalArgumentException("方阵非空且非 null");
        }
        for (double[] row : a) {
            if (row == null || row.length != a.length) {
                throw new IllegalArgumentException("方阵行列相等（实际行宽 "
                        + (row == null ? "null" : row.length) + " vs 阶 " + a.length + "）");
            }
        }
    }
}
