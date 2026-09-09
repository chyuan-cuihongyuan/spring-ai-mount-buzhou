package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日历周期预算 yml 面（spec 408 / T708，AWS Budgets calendar period 借鉴）：
 * {@code buzhou.budget.period.{enabled=false, unit=monthly, tokens-limit,
 * cost-micro-usd-limit, warning-percent=80}}。两 limit 均未配 = 配置错误
 * （装配期 fail-fast）。
 */
@ConfigurationProperties(prefix = "buzhou.budget.period")
public record BuzhouPeriodBudgetProperties(Boolean enabled,
        io.github.chyuan_cuihongyuan.buzhou.core.budget.PeriodBudgetHook.Unit unit,
        Long tokensLimit, Long costMicroUsdLimit, Integer warningPercent) {

    public BuzhouPeriodBudgetProperties {
        unit = unit == null
                ? io.github.chyuan_cuihongyuan.buzhou.core.budget.PeriodBudgetHook.Unit.MONTHLY
                : unit;
        warningPercent = warningPercent == null ? 80 : warningPercent;
    }
}
