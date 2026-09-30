package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * Householder QR 分解（spec 10009 / X10019 / impl 2412）——Householder
 * 1958 思想（「镜像反射逐列清零」——LAPACK geqrf/NumPy qr 同源）：
 * **每列构造镜像反射 H=I−βvvᵀ 把对角下元素一次清零（数值稳定
 * 优于 Gram–Schmidt 正交损失），Q=H₁⋯Hₙ 经济型显式累积，R 上三角**
 * ——最小二乘 Ax≈b 的标准直接法（法方程 AᵀA 平方条件数的病解）。
 * 高瘦阵契约（m ≥ n）；R 对角近零（秩亏）fail-fast 而非静默；
 * null/横宽阵/维数不配 fail-fast；确定性。
 */
public final class QrHouseholder {

    /** 秩亏主元容差。 */
    private static final double RANK_TOLERANCE = 1e-12;

    private final double[][] q;
    private final double[][] r;

    /**
     * 构造期分解（A 高瘦：行 ≥ 列）。
     *
     * @throws IllegalArgumentException null/空/横宽阵
     */
    public QrHouseholder(double[][] a) {
        if (a == null || a.length == 0) {
            throw new IllegalArgumentException("矩阵非空且非 null");
        }
        int m = a.length;
        int n = a[0].length;
        for (double[] row : a) {
            if (row == null || row.length != n) {
                throw new IllegalArgumentException("矩阵行宽一致（实际 "
                        + (row == null ? "null" : row.length) + " vs " + n + "）");
            }
        }
        if (m < n) {
            throw new IllegalArgumentException("高瘦阵行≥列（实际 " + m + "×" + n + "）");
        }
        double[][] work = new double[m][n];
        for (int i = 0; i < m; i++) {
            System.arraycopy(a[i], 0, work[i], 0, n);
        }
        double[][] reflectors = new double[n][];
        double[] betas = new double[n];
        for (int k = 0; k < n; k++) {
            double norm = 0.0;
            for (int i = k; i < m; i++) {
                norm += work[i][k] * work[i][k];
            }
            norm = Math.sqrt(norm);
            if (norm < RANK_TOLERANCE) {
                throw new IllegalArgumentException("矩阵满秩（列 " + k + " 范数 " + norm + "）");
            }
            double alpha = work[k][k] > 0 ? -norm : norm;
            double[] v = new double[m - k];
            for (int i = k; i < m; i++) {
                v[i - k] = work[i][k];
            }
            v[0] -= alpha;
            double vtv = 0.0;
            for (double vi : v) {
                vtv += vi * vi;
            }
            double beta = 2.0 / vtv;
            reflectors[k] = v;
            betas[k] = beta;
            work[k][k] = alpha;
            for (int i = k + 1; i < m; i++) {
                work[i][k] = 0.0;
            }
            for (int j = k + 1; j < n; j++) {
                double dot = 0.0;
                for (int i = k; i < m; i++) {
                    dot += v[i - k] * work[i][j];
                }
                double w = beta * dot;
                for (int i = k; i < m; i++) {
                    work[i][j] -= w * v[i - k];
                }
            }
        }
        this.r = work;
        this.q = new double[m][n];
        for (int j = 0; j < n; j++) {
            q[j][j] = 1.0;
        }
        for (int k = n - 1; k >= 0; k--) {
            double[] v = reflectors[k];
            double beta = betas[k];
            for (int j = 0; j < n; j++) {
                double dot = 0.0;
                for (int i = k; i < m; i++) {
                    dot += v[i - k] * q[i][j];
                }
                double w = beta * dot;
                for (int i = k; i < m; i++) {
                    q[i][j] -= w * v[i - k];
                }
            }
        }
    }

    /** 经济型 Q（m×n，列正交）。 */
    public double[][] qFactor() {
        return cloneMatrix(q);
    }

    /** 上三角 R（n×n 面域）。 */
    public double[][] rFactor() {
        return cloneMatrix(r);
    }

    /**
     * 最小二乘解 min‖Ax−b‖（先 Qᵀb 再 R x 回代）。
     *
     * @throws IllegalArgumentException 维数不配
     */
    public double[] solve(double[] b) {
        if (b == null || b.length != q.length) {
            throw new IllegalArgumentException("右端维数与行数相配（实际 "
                    + (b == null ? "null" : b.length) + " vs " + q.length + "）");
        }
        int m = q.length;
        int n = r[0].length;
        double[] qtB = new double[n];
        for (int j = 0; j < n; j++) {
            double sum = 0.0;
            for (int i = 0; i < m; i++) {
                sum += q[i][j] * b[i];
            }
            qtB[j] = sum;
        }
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = qtB[i];
            for (int j = i + 1; j < n; j++) {
                sum -= r[i][j] * x[j];
            }
            x[i] = sum / r[i][i];
        }
        return x;
    }

    private static double[][] cloneMatrix(double[][] m) {
        double[][] copy = new double[m.length][];
        for (int i = 0; i < m.length; i++) {
            copy[i] = m[i].clone();
        }
        return copy;
    }
}
