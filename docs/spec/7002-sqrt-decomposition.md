# Spec 7002 — SqrtDecomposition 分块分解（effort #7002，U3）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7205–U7206，impl 2254）。
> 借鉴：sqrt decomposition（MO's algorithm 同源思想）。

## Problem Statement

区间统计的病：每次区间扫全列 O(n)（热点区间反复求和
放大）——**块级摘要 + O(1) 点更面**缺失。

## Solution

`SqrtDecomposition`（core/metrics）：

- 值列按 ⌈√n⌉ 分块维护块级和摘要：点更新 O(1)（差分
  回写块和）、区间和 O(√n)（零整块段直读+整块段摘要点），
  无递归无 2n 冗余；
- update/rangeSum/get/size/blockSize/blockCount 读数；
  越域/倒置/空列 fail-fast。

## User Stories

1. 作为统计作者，热点区间反复求和不随列长线性放大。
2. 作为审计作者，2000 随机操作 vs 朴素扫列逐步全等。

## Testing Decisions

- n=137 随机 2000 操作（更/查混合）vs 扫列圣像；块界
  三档（1/恰整块/整块+1）；单点/全列边界；fail-fast 四路。

## Out of Scope

- 不做区间最值/批量块懒标记（和面即可）。

## Further Notes

- 与 FenwickTree（5041）同族不同面：前缀树 O(log n) vs
  分块 O(1) 点更；与 SegmentTree（5044）不同面：递归
  区间树 vs 平铺块摘要。
- 里程碑：U3/50（6%）。
