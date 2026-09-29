package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import java.util.Random;

/**
 * 别名法加权采样（spec 8025 / V8051 / impl 2327）——
 * Walker 1977/Vose 1991 思想：**预处理 O(n) 把权重折叠进
 * 等宽桶（概率列+别名列），每采样一次均匀掷+一次别名掷
 * O(1)**——逐轮扫权重 O(n) 或前缀二分 O(log n)（高频采样
 * 放大）的病解。概率缩放 1.0 域+亏空/溢出两队列装桶；负权/
 * 全零/空 fail-fast；种子注入（同种子同序列可回放）。
 *
 * <p>与 ThompsonSampler（spec 8024）同族不同面：后验探索
 * vs 权重精确采样。
 */
public final class AliasMethod {

    private final double[] probabilities;
    private final int[] aliases;
    private final Random random;
    private final double totalWeight;

    private AliasMethod(double[] probabilities, int[] aliases, Random random, double totalWeight) {
        this.probabilities = probabilities;
        this.aliases = aliases;
        this.random = random;
        this.totalWeight = totalWeight;
    }

    /** 构建（null/空/负权/全零 fail-fast）。 */
    public static AliasMethod of(double[] weights, long seed) {
        if (weights == null || weights.length == 0) {
            throw new IllegalArgumentException("权重表非空");
        }
        double total = 0;
        for (double weight : weights) {
            if (weight < 0) {
                throw new IllegalArgumentException("权重非负（实际 " + weight + "）");
            }
            total += weight;
        }
        if (total <= 0) {
            throw new IllegalArgumentException("权重和为正（实际 " + total + "）");
        }
        int n = weights.length;
        double[] probabilities = new double[n];
        int[] aliases = new int[n];
        double[] scaled = new double[n];
        java.util.ArrayDeque<Integer> small = new java.util.ArrayDeque<>();
        java.util.ArrayDeque<Integer> large = new java.util.ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            scaled[i] = weights[i] / total * n;
            if (scaled[i] < 1) {
                small.add(i);
            } else {
                large.add(i);
            }
        }
        while (!small.isEmpty() && !large.isEmpty()) {
            int s = small.poll();
            int l = large.poll();
            probabilities[s] = scaled[s];
            aliases[s] = l;
            scaled[l] -= (1 - scaled[s]);
            if (scaled[l] < 1) {
                small.add(l);
            } else {
                large.add(l);
            }
        }
        while (!large.isEmpty()) {
            probabilities[large.poll()] = 1;
        }
        while (!small.isEmpty()) {
            probabilities[small.poll()] = 1;
        }
        return new AliasMethod(probabilities, aliases, new Random(seed), total);
    }

    /** O(1) 采样一次（返回权重下标）。 */
    public int sample() {
        int bucket = random.nextInt(probabilities.length);
        double coin = random.nextDouble();
        return coin < probabilities[bucket] ? bucket : aliases[bucket];
    }

    /** 权重和（审计面）。 */
    public double totalWeight() {
        return totalWeight;
    }
}
