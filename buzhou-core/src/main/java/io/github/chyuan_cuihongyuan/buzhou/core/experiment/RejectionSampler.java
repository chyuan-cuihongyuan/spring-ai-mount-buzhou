package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import java.util.Random;
import java.util.function.DoubleUnaryOperator;

/**
 * 拒绝采样（spec 8027 / V8055 / impl 2329）——
 * von Neumann 1951 思想：**域内均匀候选 x + 均匀纵坐标
 * U·包络 ≤ f(x) 收下否则重掷**——无解析逆 CDF 的非标准
 * 分布采样（专用解析变换不可得时的通用解）。包络 M ≥ max f
 * 为前提；采样中发现 f(x)>M 即包络不足——诚实拒绝（尝试
 * 上限兜底）；密度负值 fail-fast；种子注入可回放。
 *
 * <p>与 Box-Muller 族同族不同面：专用解析变换 vs 通用包络
 * 拒绝。
 */
public final class RejectionSampler {

    private static final int MAX_ATTEMPTS = 1_000_000;

    private final DoubleUnaryOperator density;
    private final double domainMin;
    private final double domainMax;
    private final double envelope;
    private final Random random;

    private RejectionSampler(DoubleUnaryOperator density, double domainMin,
                             double domainMax, double envelope, Random random) {
        this.density = density;
        this.domainMin = domainMin;
        this.domainMax = domainMax;
        this.envelope = envelope;
        this.random = random;
    }

    /** 构建（null 密度/域倒置/包络 ≤0 fail-fast）。 */
    public static RejectionSampler of(DoubleUnaryOperator density, double domainMin,
                                      double domainMax, double envelope, long seed) {
        if (density == null) {
            throw new IllegalArgumentException("密度函数非空引用");
        }
        if (domainMin >= domainMax) {
            throw new IllegalArgumentException("采样域正区间（" + domainMin + "≥" + domainMax + "）");
        }
        if (envelope <= 0) {
            throw new IllegalArgumentException("包络为正（实际 " + envelope + "）");
        }
        return new RejectionSampler(density, domainMin, domainMax, envelope, new Random(seed));
    }

    /** 采样一个值（包络不足或尝试上限 fail-fast）。 */
    public double sample() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double candidate = domainMin + (domainMax - domainMin) * random.nextDouble();
            double target = density.applyAsDouble(candidate);
            if (target < 0) {
                throw new IllegalArgumentException("密度非负（x=" + candidate + " f=" + target + "）");
            }
            if (target > envelope * (1 + 1e-9)) {
                throw new IllegalStateException("包络不足（f(" + candidate + ")=" + target
                        + " > M=" + envelope + "）");
            }
            if (random.nextDouble() * envelope <= target) {
                return candidate;
            }
        }
        throw new IllegalStateException("尝试上限耗尽（" + MAX_ATTEMPTS + "——包络常数过小或域过宽）");
    }
}
