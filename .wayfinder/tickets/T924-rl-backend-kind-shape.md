---
id: T924
title: 限流后端形态进健康面的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

smoothing=gcra 声明后（spec 614）「是否真生效」要读代码/实验——健康面无后端形态字段。补吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 38 轮 = effort #600 / spec 637 / impl 490）：

1. `ResilienceStats.updateRateLimitBackend(kind)`（configure 时一次性写入，limiter.backend().kind()）+ `details().rateLimitBackend`。
2. 取值闭集：memory / memory-gcra / redis（共享后端 kind）/ none（未配置限流）——GCRA 声明是否生效（含被共享后端覆盖的场景）一读便知。
