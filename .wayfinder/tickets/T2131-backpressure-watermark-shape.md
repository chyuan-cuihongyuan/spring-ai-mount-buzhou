---
id: T2131
title: 事件总线积压水位读数（EventBackpressureStats）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 16 轮：事件积压历史水位面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：EventBusStats.queueDepth 瞬时值无历史；BLOCK 首推失败进限时等待是背压信号但无计数。

形状裁决：EventBackpressureStats 进程级静态面（公共类）——depthWatermark（accumulateAndGet max）+blockedPushes+Snapshot/resetForTest；埋点 BufferedEventDispatcher 入队路径三处深度采样+一处阻塞计数（只增记账行为逐位不变）；EventBusStats 公共 record 不改形。多会话进程级共享（分桶属基数红线）。

Out of scope：会话分桶；告警联动；SYNC 模式。
