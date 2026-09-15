---
id: T2863
title: 对冲延迟策略的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

等多久才值得发对冲请求？（spec 1831 / effort #1831 / R32）

## Resolution

**Google Tail at Scale hedged requests 思想纯裁决 `HedgeDelayPolicy`
（buzhou-resilience）**：hedgeThresholdMillis 样本充足取最近秩分位
（nearest-rank 确定性）、不足退守地板（无数据不冒进）；decide 边界含上
（elapsed≥threshold 即 SEND_HEDGE）。默认 P95/10ms/20 样本常量。纯裁决
零执行，与 HedgedChatModel 执行器配对。

