---
id: T6250
title: T 会话 T25 MPSC 有界队列的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6249]
created: 2026-09-26
---

## Question

T25 合同怎么逐一验绿？（spec 6024 / effort #6024 / T25）

## Resolution

**验证通过**：MpscQueueTest 四测全绿——4×2000 多生产者
不丢不重+单产内保序；容量竞争上界；单线程 FIFO+满拒新；
fail-fast。
