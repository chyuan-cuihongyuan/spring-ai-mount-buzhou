---
id: T2691
title: 重试抖动实效读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

RetrySpreadStats 的形状怎么裁决？（spec 1745 / effort #1745 / R46）（spec 1745 验收/裁决）

## Resolution

静态纯函数 analyze(delays)→SpreadReport(count/mean/relativeSpread=(max−min)/mean/min/max)；n<2 −1；负值忽略；mean=0 记 0——AWS jitter 思想，散布≈0=jitter 失效惊群风险。
