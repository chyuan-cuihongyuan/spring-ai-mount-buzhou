package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

/**
 * 多类型载荷预算分账（spec 1897 / T2995 / impl 1498）——加权水填
 * （water-filling）：每轮按剩余预算与未满足类型权重给出轮级公平
 * 份额（每权重单位额度），需求 ≤ 本轮份额的类型取所需退出、余量
 * 下轮回流给未满足者；份额放不满时按份额授予、整数零头诚实披露。
 *
 * <p>纯函数零状态；确定性（同需求同权重同预算同输出）。
 */
public final class PayloadBudgetSplit {

    private PayloadBudgetSplit() {
    }

    /** 分账结果：逐类授予字节 + 未满足需求合计（Σ(demand−grant)，诚实披露）。 */
    public record Allocation(long[] grants, long unmetDemand) {
    }

    /**
     * 加权水填分配。契约：budget ≥ 0、demand ≥ 0、weight ≥ 1、
     * demands/weights 同长且非空（fail-fast）。
     */
    public static Allocation allocate(long budgetBytes, long[] demands,
                                      long[] weights) {
        if (budgetBytes < 0) {
            throw new IllegalArgumentException("budget 不能为负：" + budgetBytes);
        }
        if (demands == null || weights == null || demands.length == 0
                || demands.length != weights.length) {
            throw new IllegalArgumentException(
                    "demands/weights 须同长且非空");
        }
        int n = demands.length;
        long[] grants = new long[n];
        boolean[] satisfied = new boolean[n];
        long remaining = budgetBytes;
        int open = n;
        for (int i = 0; i < n; i++) {
            if (demands[i] < 0) {
                throw new IllegalArgumentException("需求不能为负：" + demands[i]);
            }
            if (weights[i] < 1) {
                throw new IllegalArgumentException("权重不能小于 1：" + weights[i]);
            }
            if (demands[i] == 0) {
                satisfied[i] = true;
                open--;
            }
        }
        while (open > 0 && remaining > 0) {
            long weightedSum = 0;
            for (int i = 0; i < n; i++) {
                if (!satisfied[i]) {
                    weightedSum += weights[i];
                }
            }
            long perWeight = remaining / weightedSum;
            if (perWeight == 0) {
                break; // 剩余不足以再分一轮——零头披露
            }
            boolean exited = false;
            for (int i = 0; i < n; i++) {
                if (satisfied[i]) {
                    continue;
                }
                long share = perWeight * weights[i];
                if (demands[i] - grants[i] <= share) {
                    remaining -= (demands[i] - grants[i]);
                    grants[i] = demands[i];
                    satisfied[i] = true;
                    open--;
                    exited = true;
                }
            }
            if (!exited) {
                for (int i = 0; i < n; i++) {
                    if (!satisfied[i]) {
                        long share = perWeight * weights[i];
                        grants[i] += share;
                        remaining -= share;
                    }
                }
                break; // 本轮按份额放满——零头披露
            }
        }
        long unmet = 0;
        for (int i = 0; i < n; i++) {
            unmet += demands[i] - grants[i];
        }
        return new Allocation(grants, unmet);
    }
}
