---
id: T2119
title: 多租户配额公平指数（FairnessIndex）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 10 轮（换题轮）：租户用量公平性度量面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：原题令牌桶水位与 InMemoryRateLimitBackend.available()+RateLimitKeyHotspot 半撞——换入 R20 题（grep -i jain/fairness 零命中；gini 已在 H 805 不重复）。

形状裁决：FairnessIndex 纯函数（core/budget）——Jain 指数 J=(Σx)²/(n·Σx²)+dominantShare+shares 降序（平局典序）+isFair（FAIR_FLOOR=0.9）+全零/空 -1 哨兵；x 轴口径调用方声明；of(long[]) 位置命名重载。

Out of scope：采样接线；配额再平衡；gini（H 805 已有）。
