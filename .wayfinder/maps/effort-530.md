# Wayfinder Map — Buzhou per-model 预算闸（effort #530，E 会话第 31 轮）

> E 会话第 31 轮（budget 族模型维度扩散轮）。勘察：预算面有会话级
> （16/338）/虚拟 key（124/148）/period（408）——**模型维度**空白：
> 单模型费用失控（某模型单价暴涨/某个 agent 滥用强模型）无独立闸。
> ModelCostLedger（174/176）已按模型记账。

## Destination

`budget.ModelBudgetGate implements BuzhouHook`（order 305）：yml
`buzhou.budget.model-budget.<model> = microUsd`——exhausted() 对照
ModelCostLedger.costOf(model)，耗尽 beforeModel block（结构化告示带
模型名/已记账/预算/修法）；以记账面为准（未喂账恒 0 恒放行）；map 非空
Binder 根绑定才装配（装配审计：单 Map 组件 record 构造绑定在
prefix.<组件名> 子路径——根前缀 yml 必须根绑定直读，409/505 同型待审）。

## Notes

- 号段：spec 530 / T813–814 / impl-433。
- 借鉴源：AWS Budgets per-budget action / LiteLLM max_budget_per_model。

## Out of scope

- 滚动窗预算（记账总量口径）；自动降级到备模型（15 链可组合）；
  多模型聚合预算。

## Tickets

- [x] [T813 决策面与拦截](../tickets/T813-model-budget-gate.md)
- [x] [T814 根绑定装配](../tickets/T814-model-budget-assembly.md)
