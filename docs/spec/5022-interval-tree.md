# Spec 5022 — 区间树 stabbing 查询（effort #5022，S23）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6145–T6146，impl 2173）。
> 借鉴：居中区间树（CLRS interval tree——中点分桶 + 列表下推）。

## Problem Statement

区间重叠查询的病：全量线性扫（每查 O(n)）或自造重叠判断
（无树结构、查询语义漂移）——**O(log n + k) stabbing
查询面**缺失。

## Solution

`IntervalTree`（core/metrics）：

- 构建：区间按中点分桶递归建树——含中点者入本节点列表
 （start 升序），全在左/右者下推子树；空域终止；
- `stabbing(point)`：所有包含 point 的区间（结果按 start,end
  字典序——确定性）；点 < 中点走左+节点 start≤point 者；
  点 > 中点走右+节点 end≥point 者；等中点全收；
- fail-fast：倒置区间（start>end）/null。

## User Stories

1. 作为时间窗/号码段作者，点查询命中所有重叠区间——
   不全量扫。
2. 作为审计作者，同树同查询同结果（确定性可回放）。

## Testing Decisions

- 固定区间集 stabbing vs 线性扫圣像全等（含边界点/中点/
  稀疏域）；空树返回空；倒置区间 fail-fast；确定性回放。

## Out of Scope

- 不做动态插入/删除（静态构建口径）；不做区间树增强
 （max 端点剪枝注记）；不做多维区间。

## Further Notes

- 与 SweepLineIntervals（扫线）同族不同面：扫线全序列 vs
  树 stabbing 点查。Wave 4 第五件。
- 里程碑：S23/50（46%）。
