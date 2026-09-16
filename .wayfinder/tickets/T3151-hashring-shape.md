---
id: T3151
title: 一致性哈希环的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

键→节点归属怎么在节点增减时最小迁移？（spec 2025 / effort #2025 / R26）

## Resolution

**Dynamo/Ketama 线程安全虚节点环 `ConsistentHashRing`（core/policy）**：
addNode 铺 160 虚节点（node#i 散列均匀弧段）+nodeFor 顺时针 ceiling
归属（回绕）+删节点只迁其弧段（迁移量=原份额 ≈1/n 非全量）+加节点
只吸收近段+FNV/splitmix64 确定性散列+nodeCount/virtualNodeCount
环容量对账面。
