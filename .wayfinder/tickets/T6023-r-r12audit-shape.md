---
id: T6023
title: R 会话 R12 周期对账的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

Wave 2 五新类型的快照/档案/台账怎么对齐？（spec 4011 / effort #4011 / R12）

## Resolution

**对账轮收口**：快照补登 1133→1138（Huffman/Crc32C/DeltaFoR/
Crockford/UuidV7——message×4+concurrent×1 两段）；api-surface.md
同步 +5 行；CONTEXT 计数 1133→1138；全仓 16 模块离线 verify 三门
绿；台账核账 4000–4010 十一轮零缺位。
