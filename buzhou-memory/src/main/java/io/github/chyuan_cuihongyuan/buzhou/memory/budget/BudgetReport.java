package io.github.chyuan_cuihongyuan.buzhou.memory.budget;

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
