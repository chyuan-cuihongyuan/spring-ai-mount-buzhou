package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.budget.CostForecast;
import io.github.chyuan_cuihongyuan.buzhou.core.budget.SpendRateRing;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 成本预测健康面（spec 403 / T698，AWS Budgets forecast 借鉴）：机制名
 * {@code cost-forecast}，<b>恒 UP</b>——超预算是预测不是事故（348 同口径：
 * DOWN 会误导重启/摘流量决策）；details = 窗内花费/小时速率/水平线外推/
 * 预算/projectedOver。未配预算（≤0）UNKNOWN（机制半配置）。
 */
public final class CostForecastHealth implements BuzhouHealth {

    private final SpendRateRing ring;
    private final Duration window;
    private final Duration horizon;
    private final long budgetMicroUsd;

    public CostForecastHealth(SpendRateRing ring, Duration window, Duration horizon,
            long budgetMicroUsd) {
        this.ring = ring;
        this.window = window;
        this.horizon = horizon;
        this.budgetMicroUsd = budgetMicroUsd;
    }

    @Override
    public String mechanism() {
        return "cost-forecast";
    }

    @Override
    public Status status() {
        return budgetMicroUsd > 0 ? Status.UP : Status.UNKNOWN;
    }

    @Override
    public Map<String, Object> details() {
        CostForecast forecast = CostForecast.of(ring, window, horizon, budgetMicroUsd);
        Map<String, Object> details = new LinkedHashMap<>();
        if (budgetMicroUsd <= 0) {
            details.put("reason", "budget-not-configured");
        }
        details.put("window-spend-micro-usd", forecast.windowMicroUsd());
        details.put("rate-per-hour-micro-usd", forecast.ratePerHourMicroUsd());
        details.put("horizon-hours", horizon.toHours());
        details.put("horizon-projected-micro-usd", forecast.horizonMicroUsd());
        details.put("budget-micro-usd", budgetMicroUsd);
        details.put("projected-over", forecast.projectedOver());
        if (budgetMicroUsd > 0) {
            details.put("note", "线性外推（窗口速率 × 水平线）——不建模趋势拐点");
        }
        return details;
    }

    /** 当前外推快照（面板/测试用）。 */
    public CostForecast forecast() {
        return CostForecast.of(ring, window, horizon, budgetMicroUsd);
    }
}
