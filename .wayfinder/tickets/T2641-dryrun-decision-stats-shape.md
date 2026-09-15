---
id: T2641
title: dry-run 决策分布的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

DryRunDecisionStats 的形状怎么裁决？（spec 1720 / effort #1720 / R21）（spec 1720 验收/裁决）

## Resolution

Decision 闭集 WOULD_RUN/WOULD_BLOCK/PLAN_ERROR+record+census(planned/wouldRun/wouldBlock/planErrors/blockRatio −1 哨兵)+resetForTest——Terraform plan 决策分布，dry-run 价值量度。
