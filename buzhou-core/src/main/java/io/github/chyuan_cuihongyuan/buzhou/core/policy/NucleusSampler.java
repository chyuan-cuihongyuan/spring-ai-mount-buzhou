package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Arrays;
import java.util.random.RandomGenerator;

/**
 * top-p 核采样（spec 3004 / T5009 / impl 2005）——nucleus sampling
 * 思想（Holtzman et al.，GPT-2 采样）：截断到**累积质量 ≥ p 的最小
 * 核**（长尾噪声免截断——不像 top-k 硬截固定个数），核内按质量比例
 * 重归一采样。p 连续插值两个极端：p→0 退化 top-1（贪心），p=1 全
 * 分布（纯 softmax）；−∞ logit 质量为零永不中（禁选免掩码）。
 *
 * <p>纯函数 + RandomGenerator 注入（全序列可回放）；keptCount 为
 * 核大小确定性读数（验证/对账面）。
 */
public final class NucleusSampler {

    /** 累积到达判定的绝对容差（浮点尘埃下「恰好 ≥p」边界稳定）。 */
    private static final double P_REACH_TOLERANCE = 1e-9;

    private NucleusSampler() {
    }

    /**
     * 核采样一个索引：softmax（max 减稳定化）→ 概率降序（同值小
     * 索引先——确定性）→ 最小前缀累积 ≥ p 截断 → 核内重归一抽取。
     *
     * @throws IllegalArgumentException logits 空 / p∉(0,1] / 全 −∞ 无可采样
     */
    public static int sample(double[] logits, double p, RandomGenerator rng) {
        requireValidArgs(logits, p);
        double[] probs = softmaxProbabilities(logits);
        int[] order = descendingOrder(probs);
        int kept = nucleusPrefixSize(probs, order, p);
        double keptMass = 0;
        for (int i = 0; i < kept; i++) {
            keptMass += probs[order[i]];
        }
        double r = rng.nextDouble() * keptMass;
        double cumulative = 0;
        for (int i = 0; i < kept; i++) {
            cumulative += probs[order[i]];
            if (r < cumulative) {
                return order[i];
            }
        }
        return order[kept - 1];
    }

    /** 核大小（累积 ≥p 的最小前缀项数；≥1——p 再小也保 top-1）。 */
    public static int keptCount(double[] logits, double p) {
        requireValidArgs(logits, p);
        double[] probs = softmaxProbabilities(logits);
        return nucleusPrefixSize(probs, descendingOrder(probs), p);
    }

    private static void requireValidArgs(double[] logits, double p) {
        if (logits == null || logits.length == 0) {
            throw new IllegalArgumentException("logits 非空");
        }
        if (p <= 0 || p > 1) {
            throw new IllegalArgumentException("p∈(0,1]：" + p);
        }
    }

    private static int nucleusPrefixSize(double[] probs, int[] order, double p) {
        double threshold = p - P_REACH_TOLERANCE;
        double cum = 0;
        for (int i = 0; i < order.length; i++) {
            cum += probs[order[i]];
            if (cum >= threshold) {
                return i + 1;
            }
        }
        return order.length;
    }

    private static double[] softmaxProbabilities(double[] logits) {
        double max = Arrays.stream(logits).max().orElseThrow();
        double[] probs = new double[logits.length];
        double sum = 0;
        for (int i = 0; i < logits.length; i++) {
            probs[i] = Math.exp(logits[i] - max);
            sum += probs[i];
        }
        if (sum <= 0) {
            throw new IllegalArgumentException("全 −∞ logits 无可采样质量");
        }
        for (int i = 0; i < probs.length; i++) {
            probs[i] /= sum;
        }
        return probs;
    }

    private static int[] descendingOrder(double[] probs) {
        Integer[] idx = new Integer[probs.length];
        Arrays.setAll(idx, i -> i);
        Arrays.sort(idx, (a, b) -> {
            int byProb = Double.compare(probs[b], probs[a]);
            return byProb != 0 ? byProb : Integer.compare(a, b);
        });
        int[] order = new int[probs.length];
        Arrays.setAll(order, i -> idx[i]);
        return order;
    }
}
