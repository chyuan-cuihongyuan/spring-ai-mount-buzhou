package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * 卡方均匀性检验（spec 7038 / U7277 / impl 2290）——Pearson
 * 1900 卡方拟合优度思想（SPC/随机数测试同源）：**χ² =
 * Σ(观测−期望)²/期望**，自由度 k−1——对种子化确定性序
 * 列做「是否均匀」的量化审计面（不假装 p 值精确——
 * 阈值由调用方给，库只算统计量与逐桶贡献）。完全确定
 * 纯函数。
 *
 * <p>与 CoefficientOfVariation（同包）同族不同面：离散度
 * 单值 vs 分桶拟合优度。
 */
public final class ChiSquareUniformity {

    private ChiSquareUniformity() {
    }

    /** 检验结果（统计量+自由度+逐桶贡献）。 */
    public static final class Result {
        final double chiSquare;
        final int degreesOfFreedom;
        final double[] contributions;

        Result(double chiSquare, int degreesOfFreedom, double[] contributions) {
            this.chiSquare = chiSquare;
            this.degreesOfFreedom = degreesOfFreedom;
            this.contributions = contributions;
        }

        public double chiSquare() {
            return chiSquare;
        }

        public int degreesOfFreedom() {
            return degreesOfFreedom;
        }

        public double[] contributions() {
            return contributions;
        }
    }

    /**
     * 拟合优度（buckets≥2；观测数≥桶数（期望 ≥1 诚实下限）；
     * 越域 fail-fast）。
     */
    public static Result uniformity(int[] observations, int buckets) {
        if (buckets < 2) {
            throw new IllegalArgumentException("桶数须 ≥2: " + buckets);
        }
        if (observations == null || observations.length < buckets) {
            throw new IllegalArgumentException("观测数须 ≥桶数（期望 ≥1 诚实下限）");
        }
        int[] counts = new int[buckets];
        for (int observation : observations) {
            if (observation < 0 || observation >= buckets) {
                throw new IllegalArgumentException("观测越桶域 [0," + buckets + "): " + observation);
            }
            counts[observation]++;
        }
        double expected = (double) observations.length / buckets;
        double chiSquare = 0;
        double[] contributions = new double[buckets];
        for (int b = 0; b < buckets; b++) {
            contributions[b] = (counts[b] - expected) * (counts[b] - expected) / expected;
            chiSquare += contributions[b];
        }
        return new Result(chiSquare, buckets - 1, contributions);
    }

    /** 便捷面：种子化整数序列转桶观测。 */
    public static int[] bucketize(List<Integer> rawValues, int rawRange, int buckets) {
        int[] out = new int[rawValues.size()];
        for (int i = 0; i < rawValues.size(); i++) {
            out[i] = rawValues.get(i) * buckets / rawRange;
        }
        return out;
    }
}
