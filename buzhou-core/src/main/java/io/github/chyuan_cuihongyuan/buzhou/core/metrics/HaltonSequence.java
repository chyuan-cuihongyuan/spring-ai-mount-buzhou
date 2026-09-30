package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Halton 逆根低差异序列（spec 10025 / X10051 / impl 2428）——Halton
 * 1964 思想（「互素基逆根数位重排」——SciPy qmc/渲染 QMC 同源）：
 * **第 d 维取第 d 个素数为基，index 的数位逆序小数化（radical
 * inverse）∈[0,1)**——无方向数字表的最简低差异序列（Sobol 的表
 * 驱动互补面）。维数受素数表长度约束；维数/索引越域 fail-fast；
 * 确定性。
 */
public final class HaltonSequence {

    /** 素数基表（首八素——维数上界锚）。 */
    private static final int[] PRIME_BASES = {2, 3, 5, 7, 11, 13, 17, 19};

    /** 支持维数上界。 */
    private static final int MAX_DIMENSIONS = PRIME_BASES.length;

    private HaltonSequence() {
    }

    /**
     * 第 index 个样本（0 基）在 dimension 维上的坐标 ∈[0,1)。
     *
     * @throws IllegalArgumentException 维数/索引越域
     */
    public static double coordinate(int dimension, long index) {
        if (dimension < 1 || dimension > MAX_DIMENSIONS) {
            throw new IllegalArgumentException("维数域 [1," + MAX_DIMENSIONS + "]（实际 " + dimension + "）");
        }
        if (index < 0) {
            throw new IllegalArgumentException("索引非负（实际 " + index + "）");
        }
        return radicalInverse(index, PRIME_BASES[dimension - 1]);
    }

    /** 批量取点（samples×dimension 行主序）。 */
    public static double[][] sample(int dimension, int samples) {
        if (samples < 0) {
            throw new IllegalArgumentException("样本数为非负（实际 " + samples + "）");
        }
        double[][] points = new double[samples][];
        for (int i = 0; i < samples; i++) {
            double[] point = new double[dimension];
            for (int d = 0; d < dimension; d++) {
                point[d] = coordinate(d + 1, i);
            }
            points[i] = point;
        }
        return points;
    }

    /** 逆根数位重排（index 的 base 进制数位逆序小数）。 */
    static double radicalInverse(long index, int base) {
        double fraction = 1.0;
        double result = 0.0;
        long remaining = index;
        while (remaining > 0) {
            fraction /= base;
            result += fraction * (remaining % base);
            remaining /= base;
        }
        return result;
    }
}
