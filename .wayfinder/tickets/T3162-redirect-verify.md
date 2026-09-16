---
id: T3162
title: 重定向预算的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3161]
created: 2026-09-17
---

## Question

RedirectBudget 合同（预算/环/锚定/畸形）怎么钉住？（spec 2030 / effort #2030 / R31）

## Resolution

**七用例一次全绿**（buzhou-tools）：预算 5 内两跳 FOLLOW / 第 3 跳
恰拒（hops=2 不超）/ 50 预算下 A→B→A 环识破（预算前，环跳不记账）/
零预算首跳 BUDGET_EXHAUSTED / 起点锚定（跳回 origin 即环，visited
不含环目标）/ 自指环立即识破 / 畸形六型（−1、null/白 start、null/空
decide）fail-fast。
