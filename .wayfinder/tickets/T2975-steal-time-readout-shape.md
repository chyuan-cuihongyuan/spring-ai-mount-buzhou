---
id: T2975
title: 窃取时间读面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

虚拟化 CPU 争用的窃取占比怎么读？（spec 1887 / effort #1887 / R88）

## Resolution`

**Linux steal time 纯计算 `StealTimeReadout`（core/metrics）**：
stealRatio（Δsteal/Δtotal 两采样差分，Δtotal=0 哨兵 0.0 诚实无
信号）+ isContended（占比 ≥ 阈值判定）。计数单调性 fail-fast
（倒退即畸形）；阈值 ≥ 0。落轮 grep 复核无占坑。
