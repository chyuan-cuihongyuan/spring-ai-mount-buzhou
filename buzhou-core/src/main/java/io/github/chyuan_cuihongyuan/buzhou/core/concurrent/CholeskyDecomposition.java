package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * Cholesky 分解（spec 10008 / X10017 / impl 2411）——Cholesky 1905
 * 思想（「对称正定 A=LLᵀ 平方根分解」——LAPACK potrf/NumPy cholesky
 * 同源）：**对称正定阵专用——对角平方根+列缩放两步递推，运算量
 * 约为 LU 一半且天然数值稳定（无需选主元）**——协方差矩阵/正规
 * 方程/高斯过程同源形态。非对称 fail-fast；非正定（对角根 ≤0）
 * fail-fast 而非静默；solve 双三角回代 O(n²)/次；determinant=
 * 对角积平方（=det(A)）；L 防御性拷贝读面；确定性。
 */
public final class CholeskyDecomposition {

    /** 对称性判定容差。 */
    private static final double SYMMETRY_TOLERANCE = 1e-12;

    private final double[][] lower;
    private final double determinantValue;

    /**
     * 构造期分解（A 须对称正定）。
     *
     * @throws IllegalArgumentException null/非方阵/非对称/非正定
     */
    public CholeskyDecomposition(double[][] a) {
        validate(a);
        int n = a.length;
        this.lower = new double[n][n];
        for (int j = 0; j < n; j++) {
            double sum = a[j][j];
            for (int k = 0; k < j; k++) {
                sum -= lower[j][k] * lower[j][k];
            }
            if (sum <= 0.0) {
                throw new IllegalArgumentException("矩阵正定（第 " + j
                        + " 主对角平方根域非正 " + sum + "）");
            }
            lower[j][j] = Math.sqrt(sum);
            for (int i = j + 1; i < n; i++) {
                double dot = a[i][j];
                for (int k = 0; k < j; k++) {
                    dot -= lower[i][k] * lower[j][k];
                }
                lower[i][j] = dot / lower[j][j];
            }
        }
        double det = 1.0;
        for (int i = 0; i < n; i++) {
            det *= lower[i][i];
        }
        this.determinantValue = det * det;
    }

    /**
     * 解 Ax=b（L y=b 前代 + Lᵀ x=y 回代）。
     *
     * @throws IllegalArgumentException 维数不配
     */
    public double[] solve(double[] b) {
        if (b == null || b.length != lower.length) {
            throw new IllegalArgumentException("右端维数与方阵阶数相配（实际 "
                    + (b == null ? "null" : b.length) + " vs " + lower.length + "）");
        }
        int n = b.length;
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = b[i];
            for (int j = 0; j < i; j++) {
                sum -= lower[i][j] * y[j];
            }
            y[i] = sum / lower[i][i];
        }
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = y[i];
            for (int j = i + 1; j < n; j++) {
                sum -= lower[j][i] * x[j];
            }
            x[i] = sum / lower[i][i];
        }
        return x;
    }

    /** det(A) =（对角积）²。 */
    public double determinant() {
        return determinantValue;
    }

    /** 下三角 L（防御性拷贝）。 */
    public double[][] lowerFactor() {
        double[][] copy = new double[lower.length][];
        for (int i = 0; i < lower.length; i++) {
            copy[i] = lower[i].clone();
        }
        return copy;
    }

    private static void validate(double[][] a) {
        if (a == null || a.length == 0) {
            throw new IllegalArgumentException("方阵非空且非 null");
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null || a[i].length != a.length) {
                throw new IllegalArgumentException("方阵行列相等（实际行宽 "
                        + (a[i] == null ? "null" : a[i].length) + " vs 阶 " + a.length + "）");
            }
            for (int j = 0; j < i; j++) {
                if (Math.abs(a[i][j] - a[j][i]) > SYMMETRY_TOLERANCE) {
                    throw new IllegalArgumentException("矩阵对称（[" + i + "][" + j
                            + "]=" + a[i][j] + " vs [" + j + "][" + i + "]=" + a[j][i] + "）");
                }
            }
        }
    }
}
