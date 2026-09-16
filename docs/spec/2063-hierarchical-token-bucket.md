# Spec 2063 — 层级令牌桶（effort #2063，R64）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3227–T3228，impl 1614）。
> 借鉴：Linux HTB（Hierarchical Token Bucket）——父顶硬顶+子桶隔离。

## Problem Statement

两级限流（租户 × 产品线 / 端点 × 模型）：单层令牌桶无法表达「子级
各自限额但合打不破父级总额」——子容量之和超父顶时，无父闸即击穿
总预算；父闸一刀切又废了子级配额。

## Solution

`HierarchicalTokenBucket`（core/backpressure，synchronized 小临界区）：

- `registerChild(id, capacity)`：子桶（合容量可超父顶——父顶兜底）；
- `tryConsume(childId, amount)`：**父剩余与子剩余双闸**——任一不足
  即拒（父空=总额硬顶——子有币也借不到；子空=自限——父富余也不
  给）；通过则双扣；
- `refill(parentAmount, childAmounts)`：周期补币双封顶（父至父容量、
  子至子容量）；`snapshot()` 对账面（(parent) + 各子余）；
- 契约：父容量 ≥ 1、子容量 ≥ 0、id 非空非重复、amount ≥ 0
  fail-fast。

## User Stories

1. 作为配额作者，租户各 100 但产品线总额 500——六个租户合打不破
   500，单租户不超 100。
2. 作为对账者，snapshot 一屏父余与各子余——哪层闸在拒可判。

## Testing Decisions

- 双扣（父 70/子 20/旁桶 50）；父顶硬顶（子合 200 父 100——父尽后
  b 借不到且未扣）；子自限（父富余子尽拒）；超补封顶；部分补累积；
  畸形九型 fail-fast。

## Out of Scope

- 不做子间匀借（borrow——隔离口径）；不做按时间自动补币（refill
  由调用方周期驱动）；三层嵌套（两层够用）。

## Further Notes

- 与单层令牌桶（webhook 限速 #718）/突发信用（1872）成限流三形态。
