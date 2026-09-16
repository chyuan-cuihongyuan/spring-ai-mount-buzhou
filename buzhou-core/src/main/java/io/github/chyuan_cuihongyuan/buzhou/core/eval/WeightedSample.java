package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * 加权无放回抽样（spec 2051 / T3203 / impl 1602）——Efraimidis-Spirakis
 * 加权水库（A-Res 算法）思想：按权重无放回抽 k 个——每元素 key =
 * u^(1/w)（u 均匀），取 key 前 k 大——**单遍 O(n log k)**，等价于逐次
 * 按剩余权重抽（不放回）的分布；权重 0 永不中。用于评估集分层采样
 * /实验组分配 / 金丝雀候选挑选。
 *
 * <p>RandomGenerator 注入（确定性可回放）；纯函数无实例状态。
 */
public final class WeightedSample {

    /** 候选：元素 + 权重（≥0；0 永不中）。 */
    public record Candidate<T>(T item, double weight) {
    }

    private WeightedSample() {
    }

    /**
     * 无放回抽 k 个：key = u^(1/wᵢ) 排序取前 k。契约：candidates 非
     * null、权重 ≥ 0 非 NaN、k ≥ 0（k ≥ 候选数 = 全取——按权重序）。
     */
    public static <T> List<T> sample(List<Candidate<T>> candidates, int k, RandomGenerator random) {
        if (candidates == null || random == null) {
            throw new IllegalArgumentException("candidates/random 不能为 null");
        }
        if (k < 0) {
            throw new IllegalArgumentException("k 须 ≥ 0：" + k);
        }
        for (Candidate<T> c : candidates) {
            if (!(c.weight() >= 0) || Double.isNaN(c.weight())) {
                throw new IllegalArgumentException("权重须 ≥ 0 非 NaN：" + c.weight());
            }
        }
        record Keyed<T>(T item, double key) {
        }
        List<Keyed<T>> keyed = new ArrayList<>(candidates.size());
        for (Candidate<T> c : candidates) {
            if (c.weight() == 0) {
                continue; // 零权永不中
            }
            double u;
            do {
                u = random.nextDouble();
            } while (u <= 0.0d || u >= 1.0d);
            keyed.add(new Keyed<>(c.item(), Math.pow(u, 1.0d / c.weight())));
        }
        keyed.sort(Comparator.comparingDouble((Keyed<T> x) -> x.key()).reversed());
        List<T> picked = new ArrayList<>(Math.min(k, keyed.size()));
        for (int i = 0; i < Math.min(k, keyed.size()); i++) {
            picked.add(keyed.get(i).item());
        }
        return picked;
    }
}
