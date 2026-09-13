---
id: T1471
title: 轮次时延分位数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 11 轮：轮次时延分位数读面（补 spec 191 自己的用户故事）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 11 轮 = effort #1010 / spec 1010 / impl 763）：缺口成立——spec 191 用户故事明言「buzhou.turn.duration 的 p95 即用户体感一轮基线」，但滚动窗口读面只有 count/avg/max/last：avg 藏尾、max 单点噪声，p95 缺位。落点 core.hook：新公共 record `TurnLatencyPercentiles(count, p50Millis, p95Millis, maxMillis)` + `TurnTimingHook.percentiles(sessionId)`（对既有 64 样本窗口排序计算，不扩不缩窗口）；R-7 线性插值 h=(n−1)·q 抽包级静态纯函数（与 spec 909 同口径、可直测）；不改正史 record `TurnStats`（组件变更即破坏构造方——加法不加破损）。
