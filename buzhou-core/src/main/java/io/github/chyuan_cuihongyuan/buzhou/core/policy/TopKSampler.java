package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Arrays;
import java.util.random.RandomGenerator;

/**
 * top-k + 温度采样（spec 3038 / T5077 / impl 2038）——LLM 采样
 * 双旋钮思想（top-k 截断 + temperature 缩放）：先取 logits 前 k
 * 名（固定宽度截断），再温度缩放 softmax（T→0 贪心 / T 大趋均匀）
 * ——与 top-p（自适应宽度截断，NucleusSampler）成对：k 管「候选
 * 集宽度」、T 管「集内锐度」，组合出宽窄冷热四象限。
 *
 * <p>纯函数 + RandomGenerator 注入；probabilities 公共读数（对账
 * 面）；−∞ 落选候选集后零概率永不中。
 */
public final class TopKSampler {

    private TopKSampler() {
    }

    /**
     * 采样：前 k 名（logit 降序，同值小索引先——确定性）截断 →
     * (l−max)/T 稳定 softmax → 归一抽取。
     *
     * @throws IllegalArgumentException logits 空 / k∉[1,len] / T ≤ 0
     */
    public static int sample(double[] logits, int k, double temperature, RandomGenerator rng) {
        double[] probs = probabilities(logits, k, temperature);
        double r = rng.nextDouble();
        double cumulative = 0;
        for (int i = 0; i < probs.length; i++) {
            cumulative += probs[i];
            if (r < cumulative) {
                return i;
            }
        }
        return probs.length - 1;
    }

    /** 候选集概率读数（截断+缩放+归一——确定性对账面）。 */
    public static double[] probabilities(double[] logits, int k, double temperature) {
        if (logits == null || logits.length == 0) {
            throw new IllegalArgumentException("logits 非空");
        }
        if (k < 1 || k > logits.length) {
            throw new IllegalArgumentException("k∈[1," + logits.length + "]：" + k);
        }
        if (!(temperature > 0)) {
            throw new IllegalArgumentException("temperature > 0：" + temperature);
        }
        Integer[] order = new Integer[logits.length];
        Arrays.setAll(order, i -> i);
        Arrays.sort(order, (a, b) -> {
            int byLogit = Double.compare(logits[b], logits[a]);
            return byLogit != 0 ? byLogit : Integer.compare(a, b);
        });
        double max = logits[order[0]];
        double[] exps = new double[logits.length];
        double sum = 0;
        for (int rank = 0; rank < k; rank++) {
            int idx = order[rank];
            exps[idx] = Math.exp((logits[idx] - max) / temperature);
            sum += exps[idx];
        }
        for (int i = 0; i < exps.length; i++) {
            exps[i] = exps[i] / sum;
        }
        return exps;
    }
}
