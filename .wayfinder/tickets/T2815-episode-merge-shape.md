---
id: T2815
title: 追限事件会话化的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

散点追限与持续超载怎么分开读？（spec 1807 / effort #1807 / R8）

## Resolution

**Prometheus 告警分组/GA session gap 会话化思想纯读面
`ViolationEpisodeMerger`（core/ratelimit）**：`merge(gap, points)` 排序后
相邻差 ≤ gap 并入同事件段（Episode start/end/hits/span，单点成段 span 0）→
MergeReport（episodes/totalHits/longest/mergedSpans + hitsPerEpisode 平均段
密度 -1 哨兵）。乱序容忍；负 gap/null 时点 fail-fast。

