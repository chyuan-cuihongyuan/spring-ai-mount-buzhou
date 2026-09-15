---
id: T2624
title: 轮间到达间隔读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2623
created: 2026-09-15
---

## Question

TurnInterArrivalStats 怎么验证？（spec 1711 验收/裁决）

## Resolution

TurnInterArrivalStatsTest：六轮间隔账目+中位/p95/偶数中位均值/<2 与 null 哨兵。
