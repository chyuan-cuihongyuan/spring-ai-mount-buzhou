---
id: T6103
title: S 会话 S2 Roaring 压缩位图的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

大整数集合怎么稠稀自适应压缩存储？（spec 5001 /
effort #5001 / S2）

## Resolution

**RoaringBitSet（core/metrics）**：高 16 位分桶，桶内 ≥4096
转位图容器、稀疏短整型数组容器（Roaring 阈值口径）；
add/remove/contains + 增量 cardinality + and/or 桶级交并；
确定性迭代序。
