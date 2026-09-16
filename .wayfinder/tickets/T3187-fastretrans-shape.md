---
id: T3187
title: 快速重传触发器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

连续重复故障信号怎么提前触发不等超时？（spec 2043 / effort #2043 / R44）

## Resolution

**TCP 3-dup-ACK 线程安全触发器 `FastRetransmitTrigger`
（buzhou-resilience circuit）**：signal 连续同 id 计数达阈值（默认 3）
即触发并清零新一轮+信号切换重计（uniqueSignals 计数——问题漂移与
聚焦两类腐化分显形）+纯信号驱动无时钟确定性+threshold ≥ 2 fail-fast。
