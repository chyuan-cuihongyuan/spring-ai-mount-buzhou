package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;

import java.util.Map;

/**
 * per-model 预算闸（spec 530 / T811，budget 族扩散——16 会话预算的模型
 * 维度面）：按 ModelCostLedger 已记账总量对照 per-model 预算（microUsd），
 * 耗尽 → beforeModel 拦截（block——本轮不再烧钱；消费者收结构化告示）。
 *
 * <p>诚实边界：以**记账面**为准（Ledger 未喂账则恒 0 恒放行——观测面
 * 驱动，非计数器硬闸）；阈值语义 = 已记账 ≥ 预算即耗尽（下一轮拦截，
 * 在飞轮不回滚——成本已发生）。
 */
public class ModelBudgetGate implements BuzhouHook {

    public static final int ORDER = 305;

    private final Map<String, Long> budgets;
    private final String modelName;
    private final ModelCostLedger ledger;

    public ModelBudgetGate(Map<String, Long> budgets, String modelName, ModelCostLedger ledger) {
        if (budgets == null || budgets.isEmpty()) {
            throw new IllegalArgumentException("模型预算表非空（空表请不装配）");
        }
        budgets.values().forEach(v -> {
            if (v == null || v <= 0) {
                throw new IllegalArgumentException("预算须为正 microUsd（当前 " + v + "）");
            }
        });
        this.budgets = Map.copyOf(budgets);
        this.modelName = modelName == null ? "unknown" : modelName;
        this.ledger = ledger;
    }

    @Override
    public String name() {
        return "ModelBudgetGate";
    }

    @Override
    public int order() {
        return ORDER;
    }

    /** 决策面：当前模型预算是否已耗尽（未声明预算的模型 false）。 */
    public boolean exhausted() {
        Long cap = budgets.get(modelName);
        if (cap == null) {
            return false;
        }
        return ledger.costOf(modelName) >= cap;
    }

    @Override
    public HookResult beforeModel(ModelCallContext ctx) {
        if (!exhausted()) {
            return HookResult.CONTINUE;
        }
        long spent = ledger.costOf(modelName);
        return HookResult.block("[MODEL-BUDGET] 模型 " + modelName + " 已记账 "
                + spent + " microUsd ≥ 预算 " + budgets.get(modelName)
                + "——本轮拦截（请调整预算 buzhou.budget.model-budget." + modelName
                + " 或更换模型）");
    }
}
