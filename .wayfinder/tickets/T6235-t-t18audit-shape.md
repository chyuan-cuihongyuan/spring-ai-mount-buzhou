---
id: T6235
title: T 会话 T18 周期对账的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

Wave 3 五新类型怎么入快照封账？（spec 6017 /
effort #6018 / T18）

## Resolution

**快照补登**：regenerateSnapshot 全 reactor 再生
（1218→1228：Wave 3×5 EliasFano/GorillaXor/Simple8b/BitPacking/
DictionaryEncoding——message + Wave 4 预载×5 KdTree/QuadTree/Geohash/HilbertCurve——policy + InterpolationSearch——concurrent）+ api-surface 同步 +5 行
+ CONTEXT 计数 +5 + 全仓 verify 三门 + 台账核账。
