---
id: T6143
title: S 会话 S22 Sparse Index 稀疏索引的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

排序块数据怎么索引量级 O(块数) 且定位诚实？（spec 5021 /
effort #5021 / S22）

## Resolution

**SparseIndex（core/metrics）**：LSM/SSTable sparse index 思想
——块（blockId, firstKey）升序注册，locate 二分找最后一个
firstKey ≤ key 的块（只承诺范围不承诺存在）；早于首块 -1；
乱序/空 fail-fast。
