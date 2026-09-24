---
id: T6191
title: S 会话 S46 Robin Hood Hash Table 劫富济贫哈希表的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

开放寻址怎么压住探测距离方差且删除无墓碑？（spec 5045 /
effort #5045 / S46）

## Resolution

**RobinHoodHashTable<K,V>（core/metrics）**：线性探测插入
时来键距离超在位键即换位（被换键携距离续探）；删除后向
搬移双索引回填（无墓碑）；0.75 负载倍容；maxProbeDistance
均衡读数；畸形 fail-fast。
