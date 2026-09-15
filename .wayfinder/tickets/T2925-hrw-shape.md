---
id: T2925
title: Rendezvous 哈希的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

分片归属的最小迁移怎么免环管理实现？（spec 1862 / effort #1862 / R63）

## Resolution`

**Highest Random Weight（HRW/Rendezvous）思想纯指派 `RendezvousHashing`
（core/cache）**：assign(key, nodes)——score=确定性混合（key×node 黄金比
扩散，布隆同口径）取最高，并列字典序最小；assignAll 全量确定性可回放；
最小迁移性数学保证（节点增删只影响原属它的 1/n 键）。O(n)/键适合节点
数十级（万级该用环——诚实边界入档）。

