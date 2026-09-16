---
id: T3131
title: min-RTT 滑窗滤波的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

对冲/超时的真时延基线怎么估？（spec 2015 / effort #2015 / R16）

## Resolution

**TCP BBR min-RTT 线程安全滑窗 `MinRttTracker`（core/metrics）**：
窗内最小（默认 10 分钟——均值被膨胀污染，最小最贴真传播时延）+过期
惰性清除（滑出后次小接管——网络恶化基线可上浮）+见新最小刷新
lastFreshMinAt（基线陈旧度显形——持平不刷新）+空窗 0 配 sampleCount
有效性判据。
