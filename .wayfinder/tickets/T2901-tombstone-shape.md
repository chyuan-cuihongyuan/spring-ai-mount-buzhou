---
id: T2901
title: 墓碑占比的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

「删了但没真删」的积累怎么账面化？（spec 1850 / effort #1850 / R51）

## Resolution`

**LSM-Tree tombstone/Cassandra compaction 思想纯读面
`TombstoneRatioReadout`（core/cleanup）**：ratioOf(live, tombstones) →
Ratio（占比+readAmplification 读放大 1/(1−ratio)——占比 0.5 即 2 倍、
全墓碑无穷；全空 ratio 0 健康态非无语义）+ shouldCompact(threshold)
边界含上（默认阈 0.2 常量）。空间账与读放大账双面。

