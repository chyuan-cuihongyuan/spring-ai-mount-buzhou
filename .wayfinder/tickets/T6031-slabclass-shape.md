---
id: T6031
title: R 会话 R16 Slab 装箱的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

混尺寸长跑碎片化怎么从分配器根除？（spec 4015 / effort #4015 / R16）

## Resolution

**SlabClassPacker（core/cache）**：Memcached slab——块尺寸按增长
因子（1.25）几何分档、item 归最小容纳档（恰界归本档），同档等槽
零外部碎片（代价档内浪费可审计 wasteRatio）；超最大块 −1 拒收；
per-class 分配记账。与 BucketTableSizing 同族不同面。
