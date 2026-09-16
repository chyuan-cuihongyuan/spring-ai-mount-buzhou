---
id: T3175
title: 同步副本追踪器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

成员同步性怎么以追上时刻锚定判定？（spec 2037 / effort #2037 / R38）

## Resolution

**Kafka ISR 线程安全追踪器 `InSyncTracker`（core/recovery）**：
caughtUp 记最后追上时刻（不回拨——迟到旧追平取 max）+isInSync 距今
< lagThreshold（恰界即掉）+inSyncSet ISR 快照（收缩计 shrinkEvent、
扩张不计——收缩才是风险信号）+register 初始同步（lastCaughtUp=0
起算）。
