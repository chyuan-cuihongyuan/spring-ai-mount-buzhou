---
id: T1113
title: 预算分位推荐的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

「预算该设多少」如何从观测推导？分位算法与样本不足语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 7 轮 = effort #806 / spec 806 / impl 559）：`BudgetRecommendation` 纯函数——最近秩 P50/P95/P99+⌈P95×(1+headroom)⌉ 推荐；<5 样本 sufficient=false+(-1) 哨兵；headroom 0..500 fail-fast；Ring 1024 FIFO 收集器。不自动改预算。
