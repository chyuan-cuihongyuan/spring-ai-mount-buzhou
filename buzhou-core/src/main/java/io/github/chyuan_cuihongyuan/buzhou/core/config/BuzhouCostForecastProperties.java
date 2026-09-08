package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 成本预测 yml 面（spec 403 / T698，AWS Budgets forecast 借鉴）：
 * {@code buzhou.budget.forecast.{enabled, window, horizon, budget-micro-usd}}。
 * enabled=true 而 budget-micro-usd ≤ 0 = 半配置（健康面 UNKNOWN——
 * 速率可见、projectedOver 无意义）。
 */
@ConfigurationProperties(prefix = "buzhou.budget.forecast")
public record BuzhouCostForecastProperties(Boolean enabled, Duration window,
        Duration horizon, Long budgetMicroUsd) {

    public BuzhouCostForecastProperties {
        window = (window == null || window.isZero() || window.isNegative())
                ? Duration.ofHours(1) : window;
        horizon = (horizon == null || horizon.isZero() || horizon.isNegative())
                ? Duration.ofHours(24) : horizon;
        budgetMicroUsd = budgetMicroUsd == null ? 0L : budgetMicroUsd;
    }
}
