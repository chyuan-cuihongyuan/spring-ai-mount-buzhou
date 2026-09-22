---
id: T3035
title: 重试风暴哨兵的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

重试占比的风暴判定怎么算？（spec 1917 / effort #1917 / R118）

## Resolution`

**SRE 重试风暴惯例纯计算 `RetryStormSentinel`
（core/backpressure）**：retryRatio（重试占比读数）+ isStorm
（占比 ≥ 阈值判定，边界含上）。total≥1/retried≤total/阈值∈(0,1]
fail-fast。与 RetryBudget 互补（预算事前 vs 哨兵事后）。落轮
grep 复核无占坑。
