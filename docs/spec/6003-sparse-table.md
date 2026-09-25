# Spec 6003 — Sparse Table 稀疏表（effort #6003，T4）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6207–T6208，impl 2204）。
> 借鉴：Bender-Farach 静态 RMQ 思想（竞赛/工业静态区间最值同源）。

## Problem Statement

静态序列区间最值的病：每查一遍线性扫（重复查询 O(n) 放大），
线段树为不发生的更新支付 O(log n) 查询与建树复杂度——
**O(1) 查询静态面**缺失。

## Solution

`SparseTable`（core/metrics，long 序列最小值）：

- 倍增表 `sparse[k][i]` = 从 i 起长 2^k 的最小值（O(n log n)
  一次性预计算，不可变）；
- `rangeMin` 查询 = 覆盖区间的两个 2^k 块取 min（**幂等聚合
  允许重叠**——k=⌊log₂(len)⌋，O(1) 两次访存）；
- 读数：size；fail-fast：null/空数组、区间倒置、越界。

## User Stories

1. 作为查询作者，百万次区间最值零对数开销——静态只读底座。
2. 作为审计作者，同一序列同一查询恒同值——确定性可回放。

## Testing Decisions

- 固定种子 17 元素数组全 136 子区间暴力圣像全等；1000 元素
  1000 随机查询 vs 线性扫全等；重复值/负值/单元素边界；
  null/空/倒置/越界 fail-fast。

## Out of Scope

- 不做区间最大/自定义聚合（幂等 min 定构）；不做可变更新
 （那是线段树的面）；不做泛型比较器。

## Further Notes

- 与 SegmentTree（S45）同族不同面：静态 O(1) 幂等重叠 vs
  动态点更新 O(log n)；与 FenwickTree（Q 系）不同面：前缀
  和可加聚合 vs 幂等 min。
- 里程碑：T4/50（8%）。
