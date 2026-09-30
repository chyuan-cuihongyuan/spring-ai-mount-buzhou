package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import java.util.Random;

/**
 * 梯度 bandit（spec 9025 / W9051 / impl 2378）——Gradient bandit
 * 思想（Sutton & Barto 教材 2.8——softmax 偏好学习同源）：**偏好
 * H 只学「相对好坏」：选中 H_a←H_a+α(r−b)(1−π_a)、未选
 * H_a←H_a−α(r−b)π_a——基线 b 消绝对尺度、softmax π 只看差值**
 * ——ε-greedy 盲均匀探索（不积累偏好）与 UCB 置信界（需均值
 * 估计）的第三条路：偏好即策略。纯函数面：probabilities
 * （softmax 读数）/select（按 π 抽臂）/update（偏好原地更新，
 * 基线入参可回放）；α>0/奖励实数域（偏好无 [0,1] 约束——与
 * EXP3 概率归一域刻意不同面）；null/空/α 越域 fail-fast。
 *
 * <p>与 Exp3Bandit（spec 9024）同族不同面：乘性指数权重 vs
 * 加性偏好梯度；与 GumbelMaxSampler（policy 域）同根不同面：
 * Gumbel 技巧抽样 vs 偏好学习闭环。
 */
public final class GradientBandit {

    private GradientBandit() {
    }

    /** softmax 概率（数值稳定 max-shift）。 */
    public static double[] probabilities(double[] preferences) {
        if (preferences == null || preferences.length == 0) {
            throw new IllegalArgumentException("偏好非空数组（≥1 臂）");
        }
        double max = preferences[0];
        for (double h : preferences) {
            if (Double.isNaN(h)) {
                throw new IllegalArgumentException("偏好有限（NaN 拒绝）");
            }
            max = Math.max(max, h);
        }
        double total = 0;
        double[] exp = new double[preferences.length];
        for (int i = 0; i < preferences.length; i++) {
            exp[i] = Math.exp(preferences[i] - max);
            total += exp[i];
        }
        double[] result = new double[preferences.length];
        for (int i = 0; i < preferences.length; i++) {
            result[i] = exp[i] / total;
        }
        return result;
    }

    /** 按 softmax 抽臂。 */
    public static int select(double[] preferences, Random random) {
        double[] probabilities = probabilities(preferences);
        double roll = random.nextDouble();
        double cumulative = 0;
        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            if (roll < cumulative) {
                return i;
            }
        }
        return probabilities.length - 1;
    }

    /**
     * 偏好梯度更新（原地；基线 b 入参——常用历史均值）。
     *
     * @throws IllegalArgumentException 臂越域、α≤0、奖励/基线 NaN
     */
    public static double[] update(double[] preferences, int chosen, double reward,
                                  double baseline, double alpha) {
        if (preferences == null || preferences.length == 0) {
            throw new IllegalArgumentException("偏好非空数组（≥1 臂）");
        }
        if (chosen < 0 || chosen >= preferences.length) {
            throw new IllegalArgumentException("臂越域（" + chosen + "/" + preferences.length + "）");
        }
        if (alpha <= 0 || Double.isNaN(alpha)) {
            throw new IllegalArgumentException("α 为正（实际 " + alpha + "）");
        }
        if (Double.isNaN(reward) || Double.isNaN(baseline)) {
            throw new IllegalArgumentException("奖励/基线有限（NaN 拒绝）");
        }
        double[] probabilities = probabilities(preferences);
        double advantage = reward - baseline;
        for (int i = 0; i < preferences.length; i++) {
            if (i == chosen) {
                preferences[i] += alpha * advantage * (1 - probabilities[i]);
            } else {
                preferences[i] -= alpha * advantage * probabilities[i];
            }
        }
        return preferences;
    }
}
