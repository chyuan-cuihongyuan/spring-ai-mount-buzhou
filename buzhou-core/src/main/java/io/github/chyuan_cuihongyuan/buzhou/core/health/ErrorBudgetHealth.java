package io.github.chyuan_cuihongyuan.buzhou.core.health;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 错误预算健康面（spec 321 / T633）：任一 scope 燃尽超阈 → DOWN（严格 DOWN
 * 语义本机制 = SLO 保卫失守——燃尽超阈即失守）；无样本 → UNKNOWN（未启用 ≠
 * DOWN 纪律）。进 312 AlertRuleEngine 机制集后由 for 持续窗吸收瞬态毛刺。
 */
public final class ErrorBudgetHealth implements BuzhouHealth {

    static final int MAX_SHOWN = 3;

    private final ErrorBudget budget;

    public ErrorBudgetHealth(ErrorBudget budget) {
        this.budget = budget;
    }

    @Override
    public String mechanism() {
        return "error-budget";
    }

    @Override
    public Status status() {
        if (!budget.hasSamples()) {
            return Status.UNKNOWN;
        }
        return budget.anyBreaching() ? Status.DOWN : Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sloPercent", budget.config().sloPercent());
        out.put("window", budget.config().window().toString());
        out.put("burnRateThreshold", budget.config().burnRateThreshold());
        List<String> top = new ArrayList<>();
        for (Map.Entry<String, Double> entry : budget.topBreaching(MAX_SHOWN)) {
            top.add(entry.getKey() + " burn=" + String.format("%.1f", entry.getValue())
                    + " (samples=" + budget.samples(entry.getKey()) + ")");
        }
        out.put("topBreaching", top);
        return java.util.Collections.unmodifiableMap(out);
    }
}
