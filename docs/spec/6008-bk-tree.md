# Spec 6008 — BK 树（effort #6008，T9）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6217–T6218，impl 2209）。
> 借鉴：Burkhard-Keller 度量树思想（拼写检查/模糊搜索同源）。

## Problem Statement

编辑距离邻域查询的病：每查询全字典 O(N·L²) 扫（词库大时
放大失控）——**度量三角剪枝索引面**缺失。

## Solution

`BkTree`（core/metrics）：

- 度量空间树：节点按「到父词编辑距离」分叉——查询时对
  节点算 d(node,word)，只下探距离 ∈ [d−r, d+r] 的分支
 （三角不等式保证剪枝不漏）；
- 动态 add（词集增量友好）；query(word, r) 结果字典序
  canonical 输出；d=0 重复词幂等不增位；
- 读数：size；fail-fast：null 词、r<0。

## User Stories

1. 作为拼写作者，错拼词的 ≤2 邻域候选零全库扫——建议面。
2. 作为审计作者，同词库同查询同候选序——确定性可回放。

## Testing Decisions

- 经典 book/back/buck/bock 钉住；150 词库×60 查询×r∈{0,1,2}
  vs 逐词距离暴力圣像全等（结果集+字典序）；重复 add 幂等；
  fail-fast。

## Out of Scope

- 不做删除/平衡重建（静态偏斜词库按插入序定构）；不做
  其他度量（编辑距离定构）。

## Further Notes

- 与 TextDistance（spec 2052）同族不同面：距离度量 vs 邻域
  索引结构；与 AhoCorasick（T7）不同面：精确多模式 vs
  近似邻域。
- 里程碑：T9/50（18%）。
