---
id: T2169
title: 延迟作业调度漂移读数（DelayedJobQueue 增量）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 35 轮：延迟作业准时性读面的形状选什么？

## Resolution

**用户常设授权 AFK（可推移）**

勘察：DelayedJobQueue（spec 413）submit→到点执行无漂移读面；调度线程饥饿致迟到静默。

形状裁决：submit 内 task 包装漂移记录（执行起点 clock.instant()−fireAt 钳 0）+DriftStats(executed/lastDrift/maxDrift)+driftStats()/resetDriftForTest；已过期补跑漂移显形正值（补偿错过窗口量化）；既有语义逐位不变。

Out of scope：分位数；告警联动；线程池调优。
