package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.function.UnaryOperator;

/**
 * 共轭梯度迭代解法（spec 10010 / X10021 / impl 2413）——Hestenes–
 * Stiefel 1952 思想（「共轭方向+残差正交递推」——SciPy cg/PETSc
 * 同源）：**SPD 阵 K 步内理论收敛（K=阶数），每步一次矩阵向量
 * 积 O(n²)——大型稀疏系统 O(n³) 直接法的病解**；接口走
 * UnaryOperator&lt;double[]&gt; 矩阵向量积（稀疏实现方自挂——稠密
 * 测试锚本地给）。非正定检测（pᵀAp ≤ 0）fail-fast；维数不配/
 * 容差/迭代上限非法 fail-fast；实际迭代步读数返回面；确定性
 * （无随机性）。
 */
public final class ConjugateGradient {

    private ConjugateGradient() {
    }

    /**
     * 迭代结果（x=解；iterations=实际迭代步）。
     */
    public record Result(double[] x, int iterations) {
    }

    /**
     * 解 Ax=b（A 对称正定，由 applyA 给出矩阵向量积）。
     *
     * @param applyA 矩阵向量积（确定性纯函数）
     * @param tolerance 相对残差收敛阈（‖r‖≤tolerance·‖b‖）
     * @param maxIterations 迭代上限
     * @throws IllegalArgumentException 维数不配/容差/上限非法/非正定
     */
    public static Result solve(UnaryOperator<double[]> applyA, double[] b,
                               double tolerance, int maxIterations) {
        int n = b == null ? -1 : b.length;
        if (n <= 0) {
            throw new IllegalArgumentException("右端非空非 null");
        }
        if (applyA == null) {
            throw new IllegalArgumentException("矩阵向量积接口非 null");
        }
        if (!(tolerance > 0.0) || !Double.isFinite(tolerance)) {
            throw new IllegalArgumentException("容差为正有限（实际 " + tolerance + "）");
        }
        if (maxIterations < 1) {
            throw new IllegalArgumentException("迭代上限为正（实际 " + maxIterations + "）");
        }
        double bNorm = Math.sqrt(dot(b, b));
        double[] x = new double[n];
        double[] r = b.clone();
        double[] p = r.clone();
        double rsOld = dot(r, r);
        if (bNorm == 0.0 || Math.sqrt(rsOld) <= tolerance * bNorm) {
            return new Result(x, 0);
        }
        for (int k = 1; k <= maxIterations; k++) {
            double[] ap = applyA.apply(p);
            if (ap == null || ap.length != n) {
                throw new IllegalArgumentException("矩阵向量积返回维数相配（实际 "
                        + (ap == null ? "null" : ap.length) + " vs " + n + "）");
            }
            double pAp = dot(p, ap);
            if (pAp <= 0.0) {
                throw new IllegalArgumentException("矩阵正定（第 " + k
                        + " 步 pᵀAp=" + pAp + "）");
            }
            double alpha = rsOld / pAp;
            for (int i = 0; i < n; i++) {
                x[i] += alpha * p[i];
                r[i] -= alpha * ap[i];
            }
            double rsNew = dot(r, r);
            if (Math.sqrt(rsNew) <= tolerance * bNorm) {
                return new Result(x, k);
            }
            double beta = rsNew / rsOld;
            for (int i = 0; i < n; i++) {
                p[i] = r[i] + beta * p[i];
            }
            rsOld = rsNew;
        }
        return new Result(x, maxIterations);
    }

    /** 稠密阵矩阵向量积测试锚（静态纯函数面）。 */
    public static UnaryOperator<double[]> denseOperator(double[][] a) {
        if (a == null || a.length == 0) {
            throw new IllegalArgumentException("方阵非空且非 null");
        }
        return v -> {
            double[] out = new double[v.length];
            for (int i = 0; i < v.length; i++) {
                double sum = 0.0;
                for (int j = 0; j < v.length; j++) {
                    sum += a[i][j] * v[j];
                }
                out[i] = sum;
            }
            return out;
        };
    }

    private static double dot(double[] u, double[] v) {
        double sum = 0.0;
        for (int i = 0; i < u.length; i++) {
            sum += u[i] * v[i];
        }
        return sum;
    }
}
