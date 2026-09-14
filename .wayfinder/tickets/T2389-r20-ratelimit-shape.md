---
id: T2389
title: R20 spill 写速率限速的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2388
created: 2026-09-15
---

## Question

N 会话第 20 轮：写盘限速硬拒还是软限速（超时放行）？

## Resolution

选 **软限速**。spill 是溢出保护路径——限速器把 spill 写失败化会把保护机制
变成故障点（保护不该比被保护的更脆）；超时放行 + degraded 计数让配置错误
可观测但无害。ReentrantLock+Condition 而非 synchronized+wait（spec 1606 系
pinning 纪律）。opt-in null=关默认零行为。
