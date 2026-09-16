package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.random.RandomGenerator;

/**
 * Gumbel-max 采样（spec 2049 / T3199 / impl 1600）——Gumbel-max trick
 *（重参数化采样经典）思想：按未归一化分数（logits）直接采样——给每
 * 个候选加独立 Gumbel 噪声（−ln(−ln(u))）取 argmax，等价于按
 * softmax(logits) 概率采样而**无需归一化**——logits 含 -∞（禁选）自动
 * 零概率；确定性 RandomGenerator 注入（可回放）。
 *
 * <p>用于路由探索采样（UCB 之外的随机化策略）/评估多样化输出。
 */
public final class GumbelMaxSampler {

    private final RandomGenerator random;

    /** 契约：random 非 null（确定性源由调用方注入）。 */
    public GumbelMaxSampler(RandomGenerator random) {
        if (random == null) {
            throw new IllegalArgumentException("random 不能为 null");
        }
        this.random = random;
    }

    /**
     * 从 logits 采样一个索引：argmax(logitsᵢ + Gumbelᵢ)；空数组
     * fail-fast；-∞ logit 永不中（Gumbel 有限加 -∞ 仍 -∞，除非全体
     * -∞——此时返回首索引作退化口径并计 degenerate）。
     */
    public synchronized int sampleIndex(double[] logits) {
        if (logits == null || logits.length == 0) {
            throw new IllegalArgumentException("logits 不能为空");
        }
        for (double v : logits) {
            if (Double.isNaN(v)) {
                throw new IllegalArgumentException("logit 不能为 NaN");
            }
        }
        int best = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < logits.length; i++) {
            double noisy = logits[i] + gumbel();
            if (noisy > bestScore) {
                bestScore = noisy;
                best = i;
            }
        }
        return best;
    }

    /** 标准 Gumbel 分布样本：−ln(−ln(u))，u ∈ (0,1)。 */
    private double gumbel() {
        double u;
        do {
            u = random.nextDouble();
        } while (u <= 0.0d || u >= 1.0d); // 拒绝 0/1——ln 域合法
        return -Math.log(-Math.log(u));
    }

    /**
     * 频率健全性读数（观测面）：对给定 logits 采样 trials 次，返回各
     * 索引的经验频率——softmax(logits) 概率的蒙特卡洛对账（测试与
     * 调参共用）。
     */
    public synchronized double[] empiricalFrequencies(double[] logits, int trials) {
        if (trials < 1) {
            throw new IllegalArgumentException("trials 须 ≥ 1：" + trials);
        }
        double[] counts = new double[logits.length];
        for (int t = 0; t < trials; t++) {
            counts[sampleIndex(logits)]++;
        }
        for (int i = 0; i < counts.length; i++) {
            counts[i] /= trials;
        }
        return counts;
    }
}
