package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Random;

/**
 * 拉丁超立方采样（spec 10026 / X10053 / impl 2429）——McKay–Beckman–
 * Conover 1979 思想（「分层置换一格一点」——SciPy qmc.LatinHypercube
 * 同源）：**每维 [0,1) 等分 n 层、层内随机一点、层序随机置换——
 * 每层恰一点保证边际全覆盖**——纯随机采样层聚集的病解互补（与
 * Sobol/Halton 确定序列不同面：种子驱动随机化）。维数/样本数越
 * 域 fail-fast；同种子同采样确定；异种子异样。
 */
public final class LatinHypercube {

    private LatinHypercube() {
    }

    /**
     * 采样（samples×dimension，∈[0,1)）。
     *
     * @throws IllegalArgumentException 维数/样本数越域
     */
    public static double[][] sample(int dimension, int samples, long seed) {
        if (dimension < 1) {
            throw new IllegalArgumentException("维数为正（实际 " + dimension + "）");
        }
        if (samples < 1) {
            throw new IllegalArgumentException("样本数为正（实际 " + samples + "）");
        }
        Random random = new Random(seed);
        double[][] points = new double[samples][dimension];
        for (int d = 0; d < dimension; d++) {
            int[] strata = shuffledStrata(samples, random);
            for (int i = 0; i < samples; i++) {
                points[i][d] = (strata[i] + random.nextDouble()) / samples;
            }
        }
        return points;
    }

    /** Fisher–Yates 层置换（种子驱动确定）。 */
    private static int[] shuffledStrata(int samples, Random random) {
        int[] strata = new int[samples];
        for (int i = 0; i < samples; i++) {
            strata[i] = i;
        }
        for (int i = samples - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = strata[i];
            strata[i] = strata[j];
            strata[j] = tmp;
        }
        return strata;
    }
}
