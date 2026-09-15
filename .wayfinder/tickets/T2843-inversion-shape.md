---
id: T2843
title: 优先级反转暴露的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

关键路径被低优持有者拖死怎么显形？（spec 1821 / effort #1821 / R22）

## Resolution

**OS 优先级反转（Mars Pathfinder 教训）思想纯读面
`PriorityInversionExposure`（core/concurrent）**：HeldResource/Waiter 事实
（rank 值小=关键，id 非空白契约）；analyze → Exposure（inversions 反转
等待数（holderRank>waiterRank）/worstRankGap 最坏差/unknownResourceWaiters
未知资源诚实账 + inversionRatio -1 哨兵）。纯读不裁决，优先级继承归宿主。

