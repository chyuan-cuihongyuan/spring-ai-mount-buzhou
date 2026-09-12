---
id: T995
title: 工具泳道排队时延观测的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

LaneLimitingToolCallback 的许可获取（tryAcquire(timeout)）零排队观测——泳道满时调用者在等、等多久、多少超时被拒，全不可见（Turn 慢的泳道因素无从归因）。排队时延观测怎么落？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 23 轮 = effort #722 / spec 722 / impl 525）：withLane 的 acquire 段 nanoTime 包裹——① 每实例 WaitStats{waited, totalWaitNanos, maxWaitNanos, timeouts} record + `waitStats()` getter（实例面观测——装饰器由装配方持有，面板经装配可达）；② 指标 `buzhou.lane.wait`（timer，tag lane=<名>——泳道集配置有界，基数守卫合规；backend 共享许可路径无名不打 tag）+ `buzhou.lane.timeout`（超时拒绌事件）。计时纯观测零行为变化（acquire 语义/超时语义不动）。借鉴 grpc server queue 时延观测（排队时间与执行时间分开计量——capacity 调参依据）。
