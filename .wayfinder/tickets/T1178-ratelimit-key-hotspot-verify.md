---
id: T1178
title: 限流键热点读数验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1177]
created: 2026-09-13
---

## Question

排序/额度精度/溢出桶如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 39 轮 = effort #837）：RateLimitKeyHotspotTest 3 例——top 排序+amountSum 30.0 精确+lastSeen/溢出桶 42.0+distinct 128+1/脏入参+top 边界空真。
