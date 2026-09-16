---
id: T3189
title: 预热斜坡的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

冷启动满速洪峰怎么预热化？（spec 2044 / effort #2044 / R45）

## Resolution

**Guava warmup limiter 不可变乘数 `WarmupRamp`（core/backpressure）**：
factorAt 预热内线性爬升（0 恰 startFactor、warmup 恰 1.0）期满恒满
速+回拨宽进按起点+warmedUp 边界判定+默认 30s/10%——乘数作用在调用
方限流器/并发闸，与突发信用正交（渐升起步 vs 突发透支）。
