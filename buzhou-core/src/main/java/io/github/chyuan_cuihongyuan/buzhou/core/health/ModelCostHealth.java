package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.budget.ModelCostLedger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模型成本健康面（spec 190 §A / T550）：恒 UP（观测面——成本高是账单议题
 * 不是健康议题，裁决在预算闸）；details = 在册模型数 + 总成本（microUsd +
 * usd 人读）+ top-8 烧钱行（model/microUsd/usd）。行数有界（健康详情纪律）；
* 台账恒在（全局旋钮 + 预算钩子自动入账——spec 176），无 disabled 态。
 */
public final class ModelCostHealth implements BuzhouHealth {

    /** 展示行封顶（有界纪律）。 */
    static final int TOP_ROWS = 8;

    @Override
    public String mechanism() {
        return "model-cost";
    }

    @Override
    public Status status() {
        return Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        ModelCostLedger ledger = ModelCostLedger.global();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ModelCostLedger.ModelCost cost : ledger.topByCost(TOP_ROWS)) {
            rows.add(Map.of(
                    "model", cost.model(),
                    "microUsd", cost.microUsd(),
                    "usd", java.math.BigDecimal.valueOf(cost.microUsd(), 6)
                            .toPlainString()));
        }
        return Map.of(
                "distinctModels", ledger.distinct(),
                "totalMicroUsd", ledger.totalMicroUsd(),
                "totalUsd", java.math.BigDecimal
                        .valueOf(ledger.totalMicroUsd(), 6).toPlainString(),
                "topCosts", rows);
    }
}
