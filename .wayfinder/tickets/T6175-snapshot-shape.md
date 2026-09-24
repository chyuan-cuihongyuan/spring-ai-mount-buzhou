---
id: T6175
title: S 会话 S38 Distributed Snapshot 一致快照的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

分布式全局状态怎么切成不丢不重的一致瞬间？（spec 5037 /
effort #5037 / S38）

## Resolution

**DistributedSnapshot（core/observability）**：Chandy-Lamport
标记法——0 号发起记状态+出边发标记；进程首标记记状态+续发；
每信道标记到达即封口（记录=快照后发出/标记前在途报文）；
result 出进程状态+信道在途；未完成拒出口；畸形 fail-fast。
