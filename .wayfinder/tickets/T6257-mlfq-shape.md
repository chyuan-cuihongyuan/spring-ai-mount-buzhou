---
id: T6257
title: T 会话 T29 MLFQ 多级反馈队列的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

混合负载怎么反馈降级防饿？（spec 6028 /
effort #6028 / T29）

## Resolution

**Mlfq（core/concurrent，源码 T24 预载）**：多级队列+量子
随级翻倍+用满降级沉底，新任务进最高级，同级 FIFO；协作式
单运行槽回报告；activeTasks/readyAt 读数；级数/量子/重复/
回报告 fail-fast。
