---
id: T1114
title: 预算分位推荐验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1113]
created: 2026-09-13
---

## Question

分位/推荐值/哨兵/环形滑窗如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 7 轮 = effort #806）：BudgetRecommendationTest 6 例——1..100 最近秩 50/95/99+推荐 114/headroom 0 即 P95+P50=30/不足哨兵+恰 5 sufficient 边界/负与 null 忽略/环 FIFO（dropped=3 修正断言：预置 2 条+灌 1024 挤 2+再 1）/域外 fail-fast。
