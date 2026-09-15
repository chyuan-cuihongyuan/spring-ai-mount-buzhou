---
id: T2897
title: Misra-Gries 素描的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

流式真热点怎么 O(k) 内存确定性检测？（spec 1848 / effort #1848 / R49）

## Resolution`

**Misra-Gries 流式 heavy hitters 经典算法纯函数 `MisraGriesSketch`
（core/observability）**：sketch(k, stream) k−1 计数器计满即全员减一
（抵消一轮投票）归零淘汰；保证一切 >N/k 项必幸存、估计 ≤ 真实 ≤ 估计+
N/k（下界口径热点不漏报）；isHeavyCandidate 候选查询；确定性可回放
（对照 Count-Min 概率口径）。LinkedHashMap 保序。

