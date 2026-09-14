package io.github.chyuan_cuihongyuan.buzhou.memory.budget;
/** spec 1529 / T2309：预算计算报告——各层分配结果与溢出信号的快照。 */

public record BudgetReport(
        int contextWindow,
        int effectiveWindow,
        int fixedOverhead,
        int historyBudget,
        int summaryTokens,
        int historyTokens,
        int estimatedTotal,
        double threshold,
        boolean compactionNeeded) {
}
