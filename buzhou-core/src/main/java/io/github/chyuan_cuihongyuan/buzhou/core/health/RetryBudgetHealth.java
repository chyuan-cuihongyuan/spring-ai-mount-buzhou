package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudget;
import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudgetHolder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 重试预算健康面（spec 348 / T687，Finagle retry budget + 水位面板
 * 思想——背压族观测成员收口）：机制名 {@code retry-budget}，
 * <b>恒 UP</b>——预算拦截是保护生效不是故障（DOWN 会误导重启/摘流量
 * 决策），denied 持续增长 = 重试风暴被挡住的可见信号。
 *
 * <p>details = 全局预算快照（balance 余量 / withdrawn 已取 / denied
 * 被拦 / minBalance 下限）；未启用重试预算（holder 空）UNKNOWN。
 */
public final class RetryBudgetHealth implements BuzhouHealth {

    @Override
    public String mechanism() {
        return "retry-budget";
    }

    @Override
    public Status status() {
        return RetryBudgetHolder.current() == null ? Status.UNKNOWN : Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        RetryBudget budget = RetryBudgetHolder.current();
        Map<String, Object> details = new LinkedHashMap<>();
        if (budget == null) {
            details.put("reason", "retry-budget-disabled");
            return details;
        }
        details.put("balance", budget.balance());
        details.put("withdrawn", budget.withdrawn());
        details.put("denied", budget.denied());
        details.put("note", "denied 增长 = 重试风暴被预算挡住（保护生效，非故障）");
        return details;
    }
}
