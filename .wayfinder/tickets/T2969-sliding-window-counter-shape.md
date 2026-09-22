---
id: T2969
title: 滑动窗口计数器的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

固定窗计数如何插值出滑窗速率并做准入判定？（spec 1884 / effort #1884 / R85）

## Resolution`

**Cloudflare sliding window counter 纯计算 `SlidingWindowCounter`
（core/ratelimit）**：estimate（prev×(1−ratio)+curr 流逝比插值——
窗初惯性防边界突发）+ wouldExceed（估计 ≥ 限值即满额拒绝）。
计数≥0/ratio∈[0,1] fail-fast。零状态可叠在既有固定窗设施上。
