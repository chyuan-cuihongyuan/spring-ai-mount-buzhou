package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.random.RandomGenerator;

/**
 * Box-Muller 高斯采样器（spec 3026 / T5053 / impl 2027）——Box &
 * Muller 1958 思想：两独立均匀 → 极坐标变换出**一对**独立标准
 * 正态：z₀=√(−2ln u₁)cos(2πu₂)、z₁=√(−2ln u₁)sin(2πu₂)——
 * 均匀源造高斯的经典件（退避抖动建模/蒙特卡洛扰动/合成噪声的
 * 地基）。无缓存口径：每样本新抽均匀对（纯函数性优先——z₁ 不
 * 藏状态，经 samplePair 双取）。
 *
 * <p>u₁ 取 1−nextDouble() ∈ (0,1]（免 log(0)）；σ=0 退化常量；
 * RandomGenerator 注入确定性回放。
 */
public final class GaussianSampler {

    /** 极坐标角系数（2π——Box-Muller 公认常数）。 */
    private static final double TWO_PI = 2 * Math.PI;

    private GaussianSampler() {
    }

    /** 标准正态 N(0,1) 单样本。 */
    public static double sample(RandomGenerator rng) {
        return samplePair(rng)[0];
    }

    /** 一般正态 N(mean, stdDev²) 单样本（σ=0 退化常量 mean）。 */
    public static double sample(double mean, double stdDev, RandomGenerator rng) {
        if (!(stdDev >= 0)) {
            throw new IllegalArgumentException("stdDev ≥ 0：" + stdDev);
        }
        if (rng == null) {
            throw new IllegalArgumentException("rng 非空");
        }
        return stdDev == 0 ? mean : mean + stdDev * sample(rng);
    }

    /** 天然产出对：Box-Muller 一次变换出一对独立标准正态。 */
    public static double[] samplePair(RandomGenerator rng) {
        if (rng == null) {
            throw new IllegalArgumentException("rng 非空");
        }
        double u1 = 1 - rng.nextDouble();   // (0,1]——免 log(0)
        double u2 = rng.nextDouble();
        double radius = Math.sqrt(-2 * Math.log(u1));
        double angle = TWO_PI * u2;
        return new double[] {radius * Math.cos(angle), radius * Math.sin(angle)};
    }
}
