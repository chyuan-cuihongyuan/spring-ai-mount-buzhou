---
id: T3007
title: 自适应抖动缓冲的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

覆盖 target 比例到达的播放延迟怎么取？（spec 1903 / effort #1903 / R104）

## Resolution`

**VoIP 自适应抖动缓冲纯计算 `JitterBuffer`（core/backpressure）**：
requiredDelay（升序第 ⌈target×n⌉ 个样本——覆盖保证下取达标延迟）+
coverageRatio（给定延迟的实际覆盖占比）。target∈(0,1]/样本非空非负
fail-fast。落轮 grep 复核环形轮转与 SmoothWeightedSequence 撞坑换
静脉。
