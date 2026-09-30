package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import java.util.Random;

/**
 * EXP3 对抗 bandit（spec 9024 / W9049 / impl 2377）——Auer 2002
 * EXP3 思想（adversarial multi-armed bandit——无分布假设的 regret
 * 界同源）：**混合分布 (1−γ)·w_i/Σw + γ/K 探索，选中臂按
 * 概率归一奖励 w←w·exp(γ·r̂/K) 指数更新**——无遗憾界
 * O(√(T·K·lnK))——随机臂假设（UCB/Thompson 的随机 bandit 界）
 * 被对手操纵时崩溃的病解。纯函数面：select（依混合分布抽臂）/
 * update（重要性加权指数更新，原地）/probabilityOf（混合分布
 * 读数）；奖励域 [0,1]、γ∈(0,1] 契约；种子化确定可回放。
 *
 * <p>与 Ucb1Selector（spec 2032）/ThompsonSampler（spec 8024）
 * 同域不同面：随机 bandit 置信上界/贝叶斯采样 vs 对抗 bandit
 * 指数权重；与 GradientBandit（spec 9025）不同面：权重乘性
 * 更新 vs 偏好加性更新。
 */
public final class Exp3Bandit {

    private Exp3Bandit() {
    }

    /**
     * 依混合分布抽臂（(1−γ)·w 归一 + γ·均匀）。
     *
     * @throws IllegalArgumentException 权重空/非正/γ 越域
     */
    public static int select(double[] weights, double gamma, Random random) {
        validate(weights, gamma);
        double total = 0;
        for (double w : weights) {
            total += w;
        }
        int k = weights.length;
        double roll = random.nextDouble();
        double cumulative = 0;
        for (int i = 0; i < k; i++) {
            cumulative += (1 - gamma) * weights[i] / total + gamma / k;
            if (roll < cumulative) {
                return i;
            }
        }
        return k - 1;
    }

    /** 混合分布读数（确定性——select 的解析面）。 */
    public static double probabilityOf(double[] weights, double gamma, int arm) {
        validate(weights, gamma);
        if (arm < 0 || arm >= weights.length) {
            throw new IllegalArgumentException("臂越域（" + arm + "/" + weights.length + "）");
        }
        double total = 0;
        for (double w : weights) {
            total += w;
        }
        return (1 - gamma) * weights[arm] / total + gamma / weights.length;
    }

    /**
     * 重要性加权指数更新（原地：w_chosen ← w·exp(γ·r/(K·p_chosen))）。
     *
     * @throws IllegalArgumentException 奖励越域 [0,1]
     */
    public static double[] update(double[] weights, int chosen, double reward, double gamma) {
        validate(weights, gamma);
        if (chosen < 0 || chosen >= weights.length) {
            throw new IllegalArgumentException("臂越域（" + chosen + "/" + weights.length + "）");
        }
        if (reward < 0 || reward > 1 || Double.isNaN(reward)) {
            throw new IllegalArgumentException("奖励域 [0,1]（实际 " + reward + "）");
        }
        double probability = probabilityOf(weights, gamma, chosen);
        weights[chosen] *= Math.exp(gamma * (reward / probability) / weights.length);
        return weights;
    }

    private static void validate(double[] weights, double gamma) {
        if (weights == null || weights.length == 0) {
            throw new IllegalArgumentException("权重非空数组（≥1 臂）");
        }
        if (gamma <= 0 || gamma > 1) {
            throw new IllegalArgumentException("γ∈(0,1]（实际 " + gamma + "）");
        }
        for (double w : weights) {
            if (w <= 0 || Double.isNaN(w)) {
                throw new IllegalArgumentException("权重为正（实际 " + w + "）");
            }
        }
    }
}
