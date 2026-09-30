package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * LU 分解（spec 10007 / X10015 / impl 2410）——Doolittle 1924 思想
 * （「部分主元 PA=LU 三元组」——LAPACK getrf/NumPy lu 同源）：
 * **一次分解多次求解——L 单位下三角、U 上三角、置换 perm 行序，
 * solve 先前代后回代 O(n²)/次 vs 高斯消元 O(n³)/次**——同一矩阵
 * 多右端场景的复用形态（与 GaussianElimination（10006）同域不同
 * 面：一次性求解 vs 分解复用）。奇异 fail-fast（主元容差同高斯）；
 * 非 null/非方阵 fail-fast；同输入同态确定。
 */
public final class LUDecomposition {

    /** 奇异性主元容差（与 GaussianElimination 同款诚实边界）。 */
    private static final double SINGULARITY_TOLERANCE = 1e-12;

    private final double[][] lower;
    private final double[][] upper;
    private final int[] permutation;
    private final double determinantValue;

    /**
     * 分解结果（PA=LU：permutation 为 A 的行序）。
     */
    public LUDecomposition(double[][] a) {
        validateSquare(a);
        int n = a.length;
        double[][] m = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, m[i], 0, n);
        }
        this.lower = new double[n][n];
        this.upper = m;
        this.permutation = new int[n];
        for (int i = 0; i < n; i++) {
            lower[i][i] = 1.0;
            permutation[i] = i;
        }
        int swaps = 0;
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(upper[row][col]) > Math.abs(upper[pivot][col])) {
                    pivot = row;
                }
            }
            if (Math.abs(upper[pivot][col]) < SINGULARITY_TOLERANCE) {
                throw new IllegalArgumentException("矩阵奇异（列 " + col
                        + " 主元绝对值 " + Math.abs(upper[pivot][col]) + "）");
            }
            if (pivot != col) {
                double[] tmpRow = upper[col];
                upper[col] = upper[pivot];
                upper[pivot] = tmpRow;
                // 教科书口径：L 只换已算乘数列（col 左侧）——整行换会把
                // 单位对角 1 搬离主对角、L 上三角区冒出假 1
                for (int j = 0; j < col; j++) {
                    double t = lower[col][j];
                    lower[col][j] = lower[pivot][j];
                    lower[pivot][j] = t;
                }
                int tmpPerm = permutation[col];
                permutation[col] = permutation[pivot];
                permutation[pivot] = tmpPerm;
                swaps++;
            }
            for (int row = col + 1; row < n; row++) {
                double factor = upper[row][col] / upper[col][col];
                lower[row][col] = factor;
                upper[row][col] = 0.0;
                for (int j = col + 1; j < n; j++) {
                    upper[row][j] -= factor * upper[col][j];
                }
            }
        }
        double det = swaps % 2 == 0 ? 1.0 : -1.0;
        for (int i = 0; i < n; i++) {
            det *= upper[i][i];
        }
        this.determinantValue = det;
    }

    /**
     * 解 Ax=b（PA 解后按置换映射回原序）。
     *
     * @throws IllegalArgumentException 维数不配
     */
    public double[] solve(double[] b) {
        if (b == null || b.length != upper.length) {
            throw new IllegalArgumentException("右端维数与方阵阶数相配（实际 "
                    + (b == null ? "null" : b.length) + " vs " + upper.length + "）");
        }
        int n = b.length;
        double[] pb = new double[n];
        for (int i = 0; i < n; i++) {
            pb[i] = b[permutation[i]];
        }
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = pb[i];
            for (int j = 0; j < i; j++) {
                sum -= lower[i][j] * y[j];
            }
            y[i] = sum;
        }
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = y[i];
            for (int j = i + 1; j < n; j++) {
                sum -= upper[i][j] * x[j];
            }
            x[i] = sum / upper[i][i];
        }
        return x;
    }

    /** 行列式 = U 对角积 × 换序奇偶号（构造期记账）。 */
    public double determinant() {
        return determinantValue;
    }

    /** 单位下三角 L（防御性拷贝）。 */
    public double[][] lowerFactor() {
        return cloneMatrix(lower);
    }

    /** 上三角 U（防御性拷贝）。 */
    public double[][] upperFactor() {
        return cloneMatrix(upper);
    }

    /** 行置换（PA=LU 的 P 序）。 */
    public int[] rowPermutation() {
        return permutation.clone();
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

    private static double[][] cloneMatrix(double[][] m) {
        double[][] copy = new double[m.length][];
        for (int i = 0; i < m.length; i++) {
            copy[i] = m[i].clone();
        }
        return copy;
    }
}
