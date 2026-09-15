---
id: T2654
title: 情节保留普查的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2653
created: 2026-09-15
---

## Question

EpisodeRetentionStats 怎么验证？（spec 1726 验收/裁决）

## Resolution

EpisodeRetentionStatsTest：空哨兵/10 存 3+2 逐+占比/reset/负 n 忽略。
