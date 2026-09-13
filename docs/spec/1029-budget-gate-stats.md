# 1029 — 模型预算闸判定分布读面

> 来源：J 会话第 30 轮 = effort #1029（[T1509](../../.wayfinder/tickets/T1509-budget-gate-stats-shape.md) / [T1510](../../.wayfinder/tickets/T1510-budget-gate-stats-verify.md) / impl 782）。与 R19/R26 同谱系：闸判定四分桶/三分桶显形；预算域（Google SRE 预算耗尽告警思想）。

## Problem Statement

ModelBudgetGate（spec 530 per-model 预算闸）beforeModel 在已记账 ≥ 预算时拦截，但判定全程零计数：预算闸检查多少次、放行多少、耗尽拦截多少不可见——「连续拦截水位」是预算配置合理性（配置过低、忘记调额）的第一信号，也是「费用异常被闸住」的事后回查依据。

## 目标

- `ModelBudgetGate` 增量（core/budget，实例级）：`checks` / `allowed` / `blocked` 三 AtomicLong——守恒不变量 **checks == allowed + blocked**。
- 嵌套 record `BudgetGateStats(long checks, long allowed, long blocked)` + `stats()` 快照。
- 判定返回值与拦截文案逐位不变（已记账数值仍由 block 文本携带）。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 按 virtualKey/会话分桶（基数纪律；记账分布已有 CostAttributionLedger rollup 面）。
- 耗尽事件的独立事件面（block 文本即模型侧反馈）。
