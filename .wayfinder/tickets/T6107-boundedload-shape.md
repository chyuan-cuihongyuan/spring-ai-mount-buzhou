---
id: T6107
title: S 会话 S4 有界负载一致哈希的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

一致哈希分片怎么热点封顶不雪崩且加节点不全量洗牌？（spec 5003 /
effort #5003 / S4）

## Resolution

**BoundedLoadRing（core/cache）**：Google bounded-loads 思想
（线性探查确定性落地）——稳定哈希起点 + 满载线性探查回退 +
单节点容量封顶；总容量耗尽 ISE 诚实拒配；removeNode 迁移
确定。
