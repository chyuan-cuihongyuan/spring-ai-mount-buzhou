---
id: T6187
title: S 会话 S44 CRDT PN-Counter 正负计数器的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

分布式计数怎么无协调且并发增量不互吞？（spec 5043 /
effort #5043 / S44）

## Resolution

**CrdtPnCounter（core/transaction）**：Riak/Redis CRDT 思想
——P/N 两 G-Counter 每节点单调表，本地增减，merge 按节点
max（交换/幂等/结合三律，乱序同步必收敛）；value=P−N 可负；
单调表审计读数；畸形 fail-fast。
