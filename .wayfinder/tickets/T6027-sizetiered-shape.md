---
id: T6027
title: R 会话 R14 尺寸分层合并的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

压实成组怎么避免新旧混压与碎片堆积两病？（spec 4013 / effort #4013 / R14）

## Resolution

**SizeTieredMergePicker（core/cleanup）**：Cassandra STCS——均值
±radius 分桶、桶员 ≥min 才成组、成组取 max 个最小（写放大封顶）、
多桶竞选取员最多；纯裁决无 IO。与 ChunkCompressionPolicy（块龄）
成压实双档。
