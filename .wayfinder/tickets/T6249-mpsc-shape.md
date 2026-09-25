---
id: T6249
title: T 会话 T25 MPSC 有界队列的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

多生产者单消费者怎么有界无锁消费？（spec 6024 /
effort #6024 / T25）

## Resolution

**MpscQueue（core/concurrent）**：预分配环+双单调序号——
生产端互斥占位、消费端单线程无锁推进；满拒新 offer false、
poll 空 null；size/capacity 读数；capacity≤0/null fail-fast。
