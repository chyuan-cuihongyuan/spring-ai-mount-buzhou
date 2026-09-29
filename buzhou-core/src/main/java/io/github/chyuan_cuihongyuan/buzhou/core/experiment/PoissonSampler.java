package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import java.util.Random;

/**
 * 泊松采样（spec 8026 / V8053 / impl 2328）——
 * Knuth 1969 乘法法思想：**L=e^−λ 连乘均匀随机数、乘积 ≤L
 * 即停、计数即样本**——手工循环算阶乘（大 λ 溢出不可承受）
 * 的病解。λ>0 越域 fail-fast（λ=0 退化样本恒 0 不在域——
 * 明示）；种子注入（同种子同序列可回放）；确定性。
 *
 * <p>与 AliasMethod（spec 8025）同族不同面：连续权重离散
 * 选择 vs 事件到达计数分布。
 */
public final class PoissonSampler {

    private PoissonSampler() {
    }

    /** 采样一个泊松(λ) 值（λ>0；越域 fail-fast）。 */
    public static int sample(double lambda, Random random) {
        if (lambda <= 0) {
            throw new IllegalArgumentException("λ 为正（实际 " + lambda + "）");
        }
        if (random == null) {
            throw new IllegalArgumentException("随机源非空引用");
        }
        double limit = Math.exp(-lambda);
        double product = 1;
        int count = 0;
        do {
            count++;
            product *= random.nextDouble();
        } while (product > limit);
        return count - 1;
    }
}
