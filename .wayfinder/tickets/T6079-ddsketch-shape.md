---
id: T6079
title: R 会话 R40 DDSketch 相对误差分位的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-24
---

## Question

任意量级的分位监控怎么内存有界同精度？（spec 4039 /
effort #4039 / R40）

## Resolution

**DdSketch（core/metrics）**：Datadog DDSketch——γ=(1+α)/(1−α)
对数桶（相对恒定桶宽）+ 分位估计 γ^idx（高估 <γ 诚实口径）
+ min/max 精确旁路 + merge 同 γ 相加；正值域/越界 fail-fast。
