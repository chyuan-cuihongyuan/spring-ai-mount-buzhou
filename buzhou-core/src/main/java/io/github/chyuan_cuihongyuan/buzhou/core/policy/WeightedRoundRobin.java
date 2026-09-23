package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 平滑加权轮询（spec 5026 / T6153 / impl 2177）——nginx
 * smooth weighted round-robin 思想：每轮各节点
 * `current += weight`，选 current 最大者发出并扣
 * totalWeight——权重节点请求**均匀插散**（{a:5,b:1,c:1} 七
 * 轮 = aabacaa，a 不扎堆），权重比精确。朴素 WRR（aaaaa bc
 * 短窗倾斜）的病解；确定性无随机。
 *
 * <p>与 DeficitRoundRobin（S28 亏空调度）同族不同面：权重
 * 平滑 vs 字节亏空。
 */
public final class WeightedRoundRobin {

    private final Map<String, Integer> weights = new LinkedHashMap<>();
    private final Map<String, Integer> currents = new LinkedHashMap<>();
    private int totalWeight;

    /** 定构（权重表非空且全 >0，否则 fail-fast）。 */
    public WeightedRoundRobin(Map<String, Integer> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("权重表非空");
        }
        weights.forEach((node, weight) -> {
            if (node == null || node.isEmpty() || weight == null || weight <= 0) {
                throw new IllegalArgumentException("节点名非空且权重 >0：" + node + "/" + weight);
            }
            this.weights.put(node, weight);
            this.currents.put(node, 0);
            this.totalWeight += weight;
        });
    }

    /** 选出下一节点（平滑插值；确定性）。 */
    public String next() {
        String best = null;
        for (String node : weights.keySet()) {
            currents.compute(node, (ignored, current) -> current + weights.get(node));
            if (best == null || currents.get(node) > currents.get(best)) {
                best = node;
            }
        }
        currents.computeIfPresent(best, (ignored, current) -> current - totalWeight);
        return best;
    }

    /** 各节点当前计重读数（确定性审计面）。 */
    public Map<String, Integer> currents() {
        return Map.copyOf(currents);
    }

    /** 节点数读数。 */
    public int size() {
        return weights.size();
    }
}
