---
id: T2623
title: 轮间到达间隔读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

TurnInterArrivalStats 的形状怎么裁决？（spec 1711 / effort #1711 / R12）（spec 1711 验收/裁决）

## Resolution

静态纯函数 analyze(turnEpochMillis)→InterArrivalReport(turns/intervalsMillis/medianMillis/p95Millis)；中位偶数取均值、p95 最近秩（与 TurnLatencyPercentiles 口径一致）；<2 轮哨兵 −1——交互节奏遥测，为空闲/采样调参供据。
