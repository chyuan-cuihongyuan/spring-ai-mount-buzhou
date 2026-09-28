# Spec 7048 — KadaneMaxSubarray 最大子段和（effort #7048，U49）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7297–U7298，impl 2300）。
> 借鉴：Kadane 1984 流式最大子段和经典思想。

## Problem Statement

最大子段和的病：全子段枚举 O(n²)（序列放大）——**前缀
延续或另起炉灶的单遍 O(n) 面**缺失。

## Solution

`KadaneMaxSubarray`（core/metrics，静态工具面）：current=
max(0,current+x)、best=max(best,current) 单遍；双语义显
式面（maxSubarray 允许空段——全负返回 0 诚实；
maxSubarrayNonEmpty 非空段——全负返回最大单元素）；
long 域；确定性纯函数；null/空 fail-fast（非空语义）。

## Testing Decisions

- 经典手锚（−2,1,−3,4,−1,2,1,−5,4→6）；全负双语义对比；
  300 随机 vs 暴力圣像（双语义）；fail-fast。

## Out of Scope

- 不做段位置还原；不做环形变体。

## Further Notes

- 与 PatienceLis（7025）同族不同面：最长递增子序列 vs
  最大权和连续段。
- 里程碑：U49/50（98%）。
