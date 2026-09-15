---
id: T2879
title: 反熵分歧账的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

主副本分歧长什么样怎么记账？（spec 1839 / effort #1839 / R40）

## Resolution

**Cassandra/Dynamo anti-entropy repair 思想纯读面
`AntiEntropyDivergence`（core/recovery）**：compare(主版本表, 副本版本表)
→ 四桶（onlyInPrimary 副本丢写/onlyInReplica 主被清/versionMismatch
冲突解候选/matched）+ repairWorkload 三型合计（修复批量依据）+
matchedRatio 一致率（空比对 -1 哨兵）。三型分歧修复动作不同（移交重放/
对账裁决/冲突解），分开数才排得了优先级。

