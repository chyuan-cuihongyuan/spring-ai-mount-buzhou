# 1134 — 预算 needed 判定分布并入

> 来源：J 会话第 114 轮 = effort #1134（[T1687](../../.wayfinder/tickets/T1687-needed-ext-shape.md) / [T1688](../../.wayfinder/tickets/T1688-needed-ext-verify.md) / impl 872）。R113 预算钳位读面深化（record 尾参追加纪律）。

## Problem Statement

R113 BudgetClampStats 覆盖钳位/正常分布，但 `compactionNeeded` 判定（total > effective*threshold）的 true/false 分布零计数——**needed=true 频次即压缩触发压力信号**缺失。

## 目标

- `BudgetClampStats` record **尾参追加** `neededTrue` / `neededFalse` 两计数。
- evaluate 里 needed 判定后落桶（needed=true → neededTrue++；else → neededFalse++）。
- stats()/resetForTest() 同步扩展。
- 既有五字段语义不变；纯追加式无破坏。

## 兼容性

record 尾参追加（0.x 语义允许）；既有调用方仅构造位置参数适配（本仓无外部调用方）。evaluate 返回语义逐位不变。

## Out of Scope

- 钳位与 needed 的交叉分桶（2x2 矩阵留后续）。
