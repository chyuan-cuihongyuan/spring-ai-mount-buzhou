package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.budget.PeriodBudgetHook;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 周期预算健康面（spec 419 / T729，VirtualKeysHealth 154 先例——恒 UP
 * 观测面：耗尽由预算闸拦截，健康面只报事实不裁决）：details = 双轨进度
 * （used/limit/pct 截断 100）+ exhausted 布尔 + resetsAt（下周期起点——
 * 回血时刻）。只读快照不推进状态；Clock 注入测试确定性。
 */
public final class PeriodBudgetHealth implements BuzhouHealth {

    private final PeriodBudgetHook hook;
    private final PeriodBudgetHook.Unit unit;
    private final Long tokensLimit;
    private final Long costMicroUsdLimit;
    private final Clock clock;

    public PeriodBudgetHealth(PeriodBudgetHook hook, PeriodBudgetHook.Unit unit,
            Long tokensLimit, Long costMicroUsdLimit, Clock clock) {
        this.hook = hook;
        this.unit = unit == null ? PeriodBudgetHook.Unit.MONTHLY : unit;
        this.tokensLimit = tokensLimit != null && tokensLimit > 0 ? tokensLimit : null;
        this.costMicroUsdLimit = costMicroUsdLimit != null && costMicroUsdLimit > 0
                ? costMicroUsdLimit : null;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public String mechanism() {
        return "period-budget";
    }

    @Override
    public Status status() {
        return hook == null ? Status.UNKNOWN : Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> details = new LinkedHashMap<>();
        if (hook == null) {
            details.put("reason", "period-budget-disabled");
            return details;
        }
        long tokens = hook.periodTokens();
        long cost = hook.periodCostMicroUsd();
        details.put("unit", unit.name().toLowerCase(Locale.ROOT));
        details.put("period", hook.periodTag());
        details.put("tokens", tokens);
        if (tokensLimit != null) {
            details.put("tokensLimit", tokensLimit);
            details.put("tokensPct", pct(tokens, tokensLimit));
            details.put("exhaustedTokens", tokens >= tokensLimit);
        }
        details.put("costMicroUsd", cost);
        if (costMicroUsdLimit != null) {
            details.put("costMicroUsdLimit", costMicroUsdLimit);
            details.put("costPct", pct(cost, costMicroUsdLimit));
            details.put("exhaustedCost", cost >= costMicroUsdLimit);
        }
        details.put("resetsAt", resetsAt().toString());
        details.put("note", "耗尽由预算闸拦截（观测面只报事实）；翻页即隐式重置");
        return details;
    }

    /** 下周期起点（回血时刻）：月=下月 1 日、周=下 epochWeek 界、日=次日。 */
    Instant resetsAt() {
        LocalDate today = LocalDate.now(clock);
        LocalDate next = switch (unit) {
            case MONTHLY -> YearMonth.from(today).plusMonths(1).atDay(1);
            case WEEKLY -> LocalDate.ofEpochDay((today.toEpochDay() / 7 + 1) * 7);
            case DAILY -> today.plusDays(1);
        };
        return next.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static int pct(long used, long limit) {
        if (limit <= 0) {
            return 0;
        }
        return (int) Math.min(100, used * 100 / limit);
    }
}
