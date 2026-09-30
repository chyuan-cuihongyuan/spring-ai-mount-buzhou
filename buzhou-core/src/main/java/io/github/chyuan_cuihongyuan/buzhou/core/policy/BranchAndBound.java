package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Arrays;

/**
 * 分支限界 0/1 背包（spec 9046 / W9093 / impl 2399）——Land–Doig
 * 1960 思想（分支限界框架——运筹学/IP 求解器同源）：**DFS 二元
 * 分支（选/不选）+ LP 松弛上界（贪心分数背包）剪枝——界 ≤ 已知
 * 最优即整枝**——全子集枚举 O(2ⁿ)（容量/价值稍大即爆）的病解。
 * 物品按价值密度降序（界最紧+确定取序）；整数域；同输入同值
 * 完全确定；null/长度不齐/负值/容量负 fail-fast。
 *
 * <p>与 BinPackBalance（同包）同域不同面：装箱可行性 vs 背包
 * 价值最优；与 TspTwoOpt（spec 9045）同根不同面：启发式逼近
 * vs 精确剪枝枚举。
 */
public final class BranchAndBound {

    private BranchAndBound() {
    }

    /**
     * 最优总价值（0/1 背包——DFS 分支限界）。
     *
     * @throws IllegalArgumentException null/长度不齐/负权/负值/负容量
     */
    public static int knapsack(int[] weights, int[] values, int capacity) {
        if (weights == null || values == null || weights.length != values.length) {
            throw new IllegalArgumentException("权值数组等长非空");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("容量非负（实际 " + capacity + "）");
        }
        int n = weights.length;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            if (weights[i] < 0 || values[i] < 0) {
                throw new IllegalArgumentException("权值非负（物品 " + i + "）");
            }
            order[i] = i;
        }
        // 密度降序（同密度按索引——界最紧+确定）
        Arrays.sort(order, (a, b) -> {
            double da = values[a] * (double) weights[b];
            double db = values[b] * (double) weights[a];
            if (da != db) {
                return Double.compare(db, da);
            }
            return Integer.compare(a, b);
        });
        int[] sortedWeights = new int[n];
        int[] sortedValues = new int[n];
        for (int i = 0; i < n; i++) {
            sortedWeights[i] = weights[order[i]];
            sortedValues[i] = values[order[i]];
        }
        return dfs(sortedWeights, sortedValues, capacity, 0, 0, 0);
    }

    private static int dfs(int[] weights, int[] values, int capacity, int index,
                           int carriedWeight, int carriedValue) {
        if (index == weights.length || carriedWeight == capacity) {
            return carriedValue;
        }
        int best = carriedValue;
        // 分支：选 index（容量允许）
        if (carriedWeight + weights[index] <= capacity) {
            int bound = relaxedUpperBound(weights, values, capacity,
                    carriedWeight + weights[index], carriedValue + values[index], index + 1);
            if (bound > best) {
                best = Math.max(best, dfs(weights, values, capacity, index + 1,
                        carriedWeight + weights[index], carriedValue + values[index]));
            }
        }
        // 分支：不选 index
        int skipBound = relaxedUpperBound(weights, values, capacity, carriedWeight, carriedValue, index + 1);
        if (skipBound > best) {
            best = Math.max(best, dfs(weights, values, capacity, index + 1, carriedWeight, carriedValue));
        }
        return best;
    }

    /** LP 松弛上界：剩余物品按密度贪心装（末件可分数）。 */
    private static int relaxedUpperBound(int[] weights, int[] values, int capacity,
                                         int carriedWeight, int carriedValue, int startIndex) {
        int remaining = capacity - carriedWeight;
        double bound = carriedValue;
        for (int i = startIndex; i < weights.length && remaining > 0; i++) {
            if (weights[i] <= remaining) {
                bound += values[i];
                remaining -= weights[i];
            } else {
                bound += values[i] * (remaining / (double) weights[i]);
                remaining = 0;
            }
        }
        return (int) Math.floor(bound + 1e-9);
    }
}
