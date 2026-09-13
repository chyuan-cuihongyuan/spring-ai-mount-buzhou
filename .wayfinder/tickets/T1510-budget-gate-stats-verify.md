---
id: T1510
title: 模型预算闸判定分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1509
created: 2026-09-14
---

## Question

J 会话第 30 轮：预算闸判定分布读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（BudgetGateStatsTest，ModelCostLedger.create 直构 + 记账驱动耗尽）：预算内调用 allowed=1；记账越限后 blocked=1（block 文案含预算与已记账值）；守恒 checks == allowed + blocked；未声明预算的模型恒放行且计 allowed（exhausted=false 语义）。定向 `mvn -pl buzhou-core test -Dtest='BudgetGateStatsTest,ModelBudgetGateTest'` 绿（后者若存在）。
