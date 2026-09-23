---
id: T6117
title: S 会话 S9 Disruptor 环形缓冲的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

高频事件排队怎么零分配且认领发布两段解耦？（spec 5008 /
effort #5008 / S9）

## Resolution

**DisruptorRingBuffer（core/backpressure）**：LMAX 思想——
预分配槽（容量 2 幂 mask 导航）+ claim/publish 两段序标
（publishCursor 只连续推进）+ tryConsume 非阻塞；单消费者
口径（gating 留后诚实入档）。
