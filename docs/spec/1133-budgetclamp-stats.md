# 1133 — 预算钳位读面

> 来源：J 会话第 113 轮 = effort #1133（[T1685](../../.wayfinder/tickets/T1685-budgetclamp-stats-shape.md) / [T1686](../../.wayfinder/tickets/T1686-budgetclamp-verify.md) / impl 871）。借鉴：信号钳位检测（钳位发生=上游配置失真信号，DSP clip 检测）。budget 域首轴。

## Problem Statement

`DefaultBudgetCalculator.evaluate` 的 `Math.max(effective - fixedOverhead, 0)` 钳位——固定开销超过有效窗口时可用预算归 0：**钳位发生频次零计数**，压缩判断在失真预算上运行无信号。

## 目标

- `DefaultBudgetCalculator` 增量（memory/budget，静态面）：三 `AtomicLong`。
  - `evaluations`：evaluate 入口；`negativeClamps`（钳位发生）；`normalBudgets`（正常正预算）。
- 嵌套 `record BudgetClampStats(long evaluations, long negativeClamps, long normalBudgets)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**evaluations = negativeClamps + normalBudgets**（每入口恰落一桶）。

## 兼容性

纯增量读面：evaluate 返回语义（含钳位值）逐位不变；静态面理由同 R46–R113 先例；无新配置项。

## Out of Scope

- schemaTokensCache 命中面（缓存效率另轴）。
- 阈值判定分布（needed 已在 BudgetReport）。
