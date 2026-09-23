---
id: T6025
title: R 会话 R13 区块剪枝的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

分块查询「读不读这块」怎么统计裁决？（spec 4012 / effort #4012 / R13）

## Resolution

**ZoneMapPruner（core/cleanup）**：DuckDB zone map/Parquet 统计
思想——每块 (min,max,nullCount) 三统计，区间重叠双侧含等保守裁决
（漏读即错读）；zonesOverlapping/Matching/WithNulls 三面 +
skipped/pruningRatio 剪枝账。O(块数) 判定换 O(块体积) IO。
