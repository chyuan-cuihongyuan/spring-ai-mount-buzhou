---
id: T5015
title: Q 会话 R8 混合逻辑时钟的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

时间戳怎么既贴物理时间又因果安全？（spec 3007 / effort #3007 / R8）

## Resolution

**HybridLogicalClock（core/concurrent）**：HLC（wall,counter）二元
组——tick 同墙计数+1/墙进清零（严格单调），observe 三路 max 合并
（远端超前吸收/落后无感/回拨不倒），物理钟 LongSupplier 注入。
因果保持单向（happens-before ⇒ 严格小于），真并发判别归
VectorClockOrder。
