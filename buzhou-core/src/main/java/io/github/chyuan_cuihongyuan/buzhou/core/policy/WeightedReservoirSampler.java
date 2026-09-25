package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Weighted Reservoir Sampler 加权蓄水池采样（spec 6039 /
 * T6277 / impl 2239）——A-Chao 加权蓄水池思想：**流式等概率
 * 按权重入样**——前 m 项直接入池，其后第 i 项以 w_i/sumW
 * 概率随机替换池中一项（替换目标均匀）——先收集全部再抽
 * （内存随流线性爆）与朴素轮盘每项 O(n) 的病解。种子化
 * Random（同种子同流同样本——确定性可回放）。
 *
 * <p>与 ReservoirSample（observability，均匀蓄水池）同族
 * 不同面：等权均匀 vs 权重入样概率；与 GaussianSampler
 * （policy）不同面：连续分布变换 vs 离散流加权选择。
 */
public final class WeightedReservoirSampler {

    private final int capacity;
    private final Random rng;
    private final List<Long> items = new ArrayList<>();
    private final List<Long> weights = new ArrayList<>();
    private double sumWeight;
    private long seenCount;

    /** 池容量+种子（capacity≤0 fail-fast；同种子同样本）。 */
    public WeightedReservoirSampler(int capacity, long seed) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("容量必须为正: " + capacity);
        }
        this.capacity = capacity;
        this.rng = new Random(seed);
    }

    /** 提交带权项（weight≤0 fail-fast；确定性替换决策）。 */
    public void offer(long item, long weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("权重必须为正: " + weight);
        }
        seenCount++;
        if (items.size() < capacity) {
            items.add(item);
            weights.add(weight);
            sumWeight += weight;
            return;
        }
        sumWeight += weight;
        if (rng.nextDouble() < (double) weight / sumWeight) {
            int victim = rng.nextInt(items.size());
            double victimWeight = weights.get(victim);
            items.set(victim, item);
            weights.set(victim, weight);
            sumWeight -= victimWeight;
        } else {
            sumWeight -= weight;
        }
    }

    /** 当前样本副本（池内插入序——确定性）。 */
    public List<Long> sample() {
        return new ArrayList<>(items);
    }

    /** 样本数读数。 */
    public int size() {
        return items.size();
    }

    /** 已见项数读数。 */
    public long seenCount() {
        return seenCount;
    }

    /** 容量读数。 */
    public int capacity() {
        return capacity;
    }
}
