---
id: T1168
title: 危险工具命中分布验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1167]
created: 2026-09-13
---

## Question

排行/溢出桶/脏入参如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 34 轮 = effort #833）：DangerousToolHitStatsTest 3 例——top 降序+lastSeen max+totalHits 全入账/溢出桶 distinct 64+1/脏入参不计 total+top 边界空真。两处账误修正（全入账口径+溢出桶占位）。
