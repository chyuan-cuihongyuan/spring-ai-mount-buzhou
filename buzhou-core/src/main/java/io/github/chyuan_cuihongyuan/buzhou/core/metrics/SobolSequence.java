package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * Sobol 低差异序列（spec 10024 / X10049 / impl 2427）——Sobol 1967
 * 思想（「方向数字异或 Gray 码」——SciPy qmc/图形学 QMC 同源）：
 * **每维一组方向数字 v[i]，样本 n 经 Gray 码 n⊕(n≫1) 选位异或得
 * 点值/2³²——前 2^m 点构成 (t,m,s)-网，一维投影逐层二分均匀**——
 * 蒙特卡洛 O(1/√N) 误差 vs 准蒙特卡洛 O((log N)^s/N) 的病解互补。
 * 方向数字表取 new-Joe-Kuo 首八维（入档）；维数/索引越域 fail-fast；
 * 确定性（无随机性）。
 */
public final class SobolSequence {

    /** 位宽（方向数字字长）。 */
    private static final int BITS = 32;

    /** 支持维数上界（方向数字表行数）。 */
    private static final int MAX_DIMENSIONS = 8;

    /** 每维初始方向数字表（new-Joe-Kuo 首八维：{s, a, m₁..m_s}）。 */
    private static final int[][] DIRECTION_TABLE = {
            {1, 0, 1},
            {2, 1, 1, 1},
            {3, 1, 1, 3, 1},
            {3, 2, 1, 1, 1},
            {4, 1, 1, 1, 3, 3},
            {4, 4, 1, 3, 5, 13},
            {4, 13, 1, 1, 5, 5},
            {5, 7, 1, 1, 7, 5, 1},
    };

    private static final int[][][] DIRECTION_NUMBERS = buildAll();

    private SobolSequence() {
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
        long gray = index ^ (index >> 1);
        int[][] directions = DIRECTION_NUMBERS[dimension - 1];
        long value = 0;
        for (int bit = 0; bit < BITS; bit++) {
            if ((gray >>> bit & 1) == 1) {
                value ^= directions[bit][0] & 0xFFFFFFFFL;
            }
        }
        return value / (double) (1L << BITS);
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

    private static int[][][] buildAll() {
        int[][][] all = new int[MAX_DIMENSIONS][][];
        for (int d = 0; d < MAX_DIMENSIONS; d++) {
            int[] row = DIRECTION_TABLE[d];
            int s = row[0];
            int a = row[1];
            int[][] v = new int[BITS][1];
            for (int i = 0; i < s; i++) {
                v[i][0] = row[2 + i] << (BITS - 1 - i);
            }
            for (int i = s; i < BITS; i++) {
                v[i][0] = v[i - s][0] ^ (v[i - s][0] >>> s);
                for (int k = 1; k < s; k++) {
                    if (((a >>> (s - 1 - k)) & 1) == 1) {
                        v[i][0] ^= v[i - k][0];
                    }
                }
            }
            all[d] = v;
        }
        return all;
    }

    /** 方向数字只读面（测试锚）。 */
    static int[][] directionNumbers(int dimension) {
        return Arrays.copyOf(DIRECTION_NUMBERS[dimension - 1], BITS);
    }
}
