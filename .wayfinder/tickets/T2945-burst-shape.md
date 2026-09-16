---
id: T2945
title: 突发信用账户的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

限流「拒绝」之外的降速第三态怎么账户化？（spec 1872 / effort #1872 / R73）

## Resolution`

**AWS CPU credit / T3 unlimited 语义线程安全账户 `BurstCreditAccount`
（core/backpressure）**：基准速率蓄水封顶（时钟回拨 fail-fast）+ trySpend
透支水位（不足拒 false——调用方降速非拒任务）+ 枯竭计数（见底一次性）+
stats 快照（水位/枯竭/满水率 + burstHeadroomMillis 余量换算）。与 GCRA
（拒绝）、PrefetchCreditWindow（在飞上限）三足。

