package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import java.time.Duration;

/**
 * 成本外推快照（spec 403 / T697，AWS Budgets forecast 借鉴——
 * actualSpend/elapsed × total 的线性外推）：窗内花费 → 小时速率 →
 * 水平线外推 → 预算对照。诚实边界：线性外推不建模趋势拐点
 * （「窗口内速率代表未来」是假设）。
 */
public record CostForecast(long windowMicroUsd, long ratePerHourMicroUsd,
        Duration horizon, long horizonMicroUsd, long budgetMicroUsd,
        boolean projectedOver) {

    /** 从速率环外推（budget ≤ 0 = 无预算——projectedOver 恒 false）。 */
    public static CostForecast of(SpendRateRing ring, Duration window, Duration horizon,
            long budgetMicroUsd) {
        long windowTotal = ring.windowTotal(window);
        long rate = ring.ratePerHour(window);
        long horizonHours = Math.max(1, horizon.toHours());
        long horizonMicroUsd = rate * horizonHours;
        return new CostForecast(windowTotal, rate, horizon, horizonMicroUsd,
                budgetMicroUsd, budgetMicroUsd > 0 && horizonMicroUsd >= budgetMicroUsd);
    }
}
