---
id: T2642
title: dry-run 决策分布的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2641
created: 2026-09-15
---

## Question

DryRunDecisionStats 怎么验证？（spec 1720 验收/裁决）

## Resolution

DryRunDecisionStatsTest：空哨兵/四笔 2:1:1+blockRatio=0.25/reset。
