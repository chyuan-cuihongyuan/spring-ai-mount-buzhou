package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.Random;

/**
 * 置换检验（spec 9027 / W9055 / impl 2380）——Fisher 1930s
 * 精确检验思想（scipy permutation_test/统计推断同源）：**合并
 * 两组、反复随机重分两组，统计置换分布中 |均值差| ≥ 观测差
 * 的比例——零假设下标签可交换的「穷举分布」蒙特卡洛近似**——
 * t 检验需正态假设（偏态/小样本失真）与 bootstrap 需重采样
 * 原样本（分布形态保持）的频率学派镜像：标签重排生成零分布。
 * 双侧 p 值（观测差计入——含自身防 p=0）；置换次数入参；
 * 种子化确定可回放；null/空组/组数不等价任一为空/次数<1
 * fail-fast。
 *
 * <p>与 WilsonInterval（同包）互补：比例置信区间 vs 均值差
 * 显著性；与 bootstrap 均值置信区间（M 系已占）同族不同面：
 * 重采样观测 vs 重排标签。
 */
public final class PermutationTest {

    private PermutationTest() {
    }

    /**
     * 双侧置换 p 值（|置换差| ≥ |观测差| 比例，含观测自身）。
     *
     * @throws IllegalArgumentException null/空数组、次数 < 1
     */
    public static double twoSidedPValue(double[] a, double[] b, int permutations, Random random) {
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            throw new IllegalArgumentException("两组非空（各 ≥1 观测）");
        }
        if (permutations < 1) {
            throw new IllegalArgumentException("置换次数为正（实际 " + permutations + "）");
        }
        int na = a.length;
        int total = na + b.length;
        double[] pooled = new double[total];
        for (int i = 0; i < na; i++) {
            pooled[i] = a[i];
        }
        for (int i = 0; i < b.length; i++) {
            pooled[na + i] = b[i];
        }
        double observed = Math.abs(meanOf(pooled, 0, na) - meanOf(pooled, na, total));
        int extreme = 1; // 观测自身计入（防 p=0——有限置换诚实界）
        for (int p = 0; p < permutations; p++) {
            shuffle(pooled, random);
            double permuted = Math.abs(meanOf(pooled, 0, na) - meanOf(pooled, na, total));
            if (permuted >= observed - 1e-12) {
                extreme++;
            }
        }
        return extreme / (double) (permutations + 1);
    }

    private static double meanOf(double[] values, int from, int to) {
        double sum = 0;
        for (int i = from; i < to; i++) {
            sum += values[i];
        }
        return sum / (to - from);
    }

    private static void shuffle(double[] values, Random random) {
        for (int i = values.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            double tmp = values[i];
            values[i] = values[j];
            values[j] = tmp;
        }
    }
}
