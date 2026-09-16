---
id: T3161
title: 重定向预算的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

HTTP 重定向跟随的双重防线怎么原语化？（spec 2030 / effort #2030 / R31）

## Resolution

**curl max-redirs × 环检测 `RedirectBudget`（buzhou-tools http，单请求
一件）**：maxRedirects ≥ 0（0 不跟随）+startFrom 锚定访问集 +decide
三态（FOLLOW 记账 / BUDGET_EXHAUSTED 跳数硬界防拖死 / LOOP_DETECTED
访问集识破环防空耗——环跳不记账）+hops/visitedCount 读数。
