---
id: T57
title: core · Turn Deadline 对象化贯穿与挂起修复
type: grilling
status: closed
assignee: ""
blocked-by: [T55]
created: 2026-08-14
---

## Question

Turn 预算如何对象化并消灭永久挂起点？需裁决：① Deadline 对象形状（绝对时刻、remaining()、isExpired，gRPC 语义）；② 工具派发取 min(perToolTimeout, turnDeadline)、嵌套调用用剩余时间而非重新计时；③ 外层 futures.get() 无超时 + synchronized(groupLock) 不可中断的修复方案（组锁改 ReentrantLock.tryLock(timeout)？acquire 改 tryAcquire(timeout)？）；④ 模型调用 core 层超时包裹（loopTimeout 默认 null 不限是否改为有限默认）；⑤ 与既有 CancellationToken 的合并形状（CancellationScope 级联）。

## Resolution

**ratify（用户常设授权，可推翻）→ spec 13 §core-2**：`TurnDeadline` 绝对时刻值对象（remaining/isExpired/组合器）；工具派发取 min(perToolTimeout, deadline.remaining())、嵌套传剩余时间（gRPC 语义）；三个永久阻塞点修复——外层 join 限时（超时=TIMEOUT 回喂）、组锁改 ReentrantLock.tryLock(timeout)、acquire 改 tryAcquire(timeout)；模型调用在 loopTimeout/Deadline 配置时受剩余时间兜底；与 CancellationToken 并列传播不合并（保既有 API）；ToolSetSpec 超时在执行器消费。
