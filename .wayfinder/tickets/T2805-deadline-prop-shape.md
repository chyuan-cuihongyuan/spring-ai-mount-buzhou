---
id: T2805
title: 轮墙钟预算传播的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

轮级预算怎么传播到逐工具调用？（spec 1802 / effort #1802 / R3）

## Resolution

**gRPC deadline propagation 三规则纯函数 `TurnDeadlineBudget`（core/exec）**：
`plan(totalNanos, estimates)` 逐调用裁决——剩余预算顺序递减（传播）、获准
超时=min(预估,剩余)（截断）、剩余=0 拒绝且零耗也不例外（deadline 先于派发
检查）；BudgetPlan 带 committedNanos 与 admissionRatio（空计划 -1 哨兵）。
纯裁决不执行，派发归宿主。

