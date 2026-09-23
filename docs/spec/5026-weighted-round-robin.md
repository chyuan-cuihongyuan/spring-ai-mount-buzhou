# Spec 5026 — 平滑加权轮询（effort #5026，S27）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6153–T6154，impl 2177）。
> 借鉴：nginx smooth weighted round-robin（当前计重 + 最大降权）。

## Problem Statement

加权分发的病：朴素 WRR（aaaaa bc——权重节点的请求突发扎堆）
或只按权重比无平滑（短窗口倾斜）——**平滑插值分发面**缺失。

## Solution

`WeightedRoundRobin`（core/policy）：

- 每轮：各节点 `current += weight`；选 current 最大者发出，
  该者 `current -= totalWeight`——**权重节点请求均匀插散**
 （nginx smooth 同口径）；
- `{a:5,b:1,c:1}` 七轮序列 = a a b a c a a——a 不扎堆；
- 读数：currents（各节点当前计重）；确定性无随机；
- fail-fast：空权重表、weight≤0。

## User Stories

1. 作为负载分发作者，权重比精确且相邻请求不扎堆同一节点。
2. 作为审计作者，同轮询序列同分发序（确定性可回放）。

## Testing Decisions

- 经典 {5,1,1} 七轮序列逐位断言；权重比精确（轮数放大后
  频次=权重比）；单节点退化恒发；空表/weight≤0 fail-fast；
  确定性回放。

## Out of Scope

- 不做健康摘除联动（健康检查族）；不做最少连接混合策略；
- 不做动态权重调整。

## Further Notes

- 与 DeficitRoundRobin（S28 亏空调度）同族不同面：权重平滑
  vs 字节亏空。Wave 5 第三件。
- 里程碑：S27/50（54%）。
