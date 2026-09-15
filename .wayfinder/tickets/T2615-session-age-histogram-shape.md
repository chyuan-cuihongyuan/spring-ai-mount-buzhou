---
id: T2615
title: 会话年龄分桶直方的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

SessionAgeHistogram 的形状怎么裁决？（spec 1707 / effort #1707 / R8）（spec 1707 验收/裁决）

## Resolution

实例面 AtomicLongArray 桶式（IdleDurationHistogram 房规镜像）：默认 1h/1d/7d→4 桶互斥+record 负值忽略+bucketCounts/total/eldestMillis 哨戒；自定义 n 边界 n+1 桶——Prometheus histogram 思想。
