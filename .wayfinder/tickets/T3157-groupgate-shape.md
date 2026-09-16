---
id: T3157
title: 并发组闸的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

同组任务的互斥与取代语义怎么定？（spec 2028 / effort #2028 / R29）

## Resolution

**GitHub Actions concurrency group 线程安全闸 `ConcurrencyGroupGate`
（core/exec）**：tryEnter 三态（GRANTED 属主/重入幂等/SUPERSEDED
cancelInProgress 新者接管旧者取消——只留最新防堆积/BUSY_REJECTED
在跑者优先）+complete **属主栅栏**（仅现属主释放，被取代者迟到完成
拦下 fencedCompletions 显形）+四计数+组间独立。
