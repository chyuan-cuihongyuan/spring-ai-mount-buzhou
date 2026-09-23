---
id: T6035
title: R 会话 R18 周期对账的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

Wave 3 五新类型的快照/档案/台账怎么对齐？（spec 4017 / effort #4017 / R18）

## Resolution

**对账轮收口**：快照补登 1138→1143（ZoneMap/SizeTiered/Bitcask/
Slab/Hnsw——cleanup×3+cache×1+memory×1 三段）；api-surface.md 同步
+5 行；CONTEXT 计数 1138→1143；全仓 16 模块离线 verify 三门绿；
台账核账 4000–4016 十七轮零缺位。
