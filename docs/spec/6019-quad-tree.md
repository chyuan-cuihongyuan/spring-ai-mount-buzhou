# Spec 6019 — QuadTree 四叉树（effort #6019，T20）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6239–T6240，impl 2220）。
> 借鉴：地理围栏/空间索引四叉树思想。源码于 T18 对账批预入档。

## Problem Statement

平面区域查询的病：每查询全点集线性过滤（O(n) 放大）——
**象限剪枝索引面**缺失。

## Solution

`QuadTree`（core/policy，源码已预载）：

- 点域四叉树：桶容量 4 超限四分（子界 [midX+1,maxX] 与
  象限判定 `> midX` 严格一致——边界点不漂移）；查询按
  「矩形与节点界相交」剪枝；
- 结果 (x,y) 字典序 canonical；退化单格根自动停分（桶
  无界增长诚实可见）；
- fail-fast：根界倒置、越界插入、查询矩形倒置。

## User Stories

1. 作为围栏作者，电子围栏命中查询对数复杂度——地理底座。
2. 作为审计作者，同点集同查询同序——确定性可回放。

## Testing Decisions

- 300 点×30 随机矩形 vs 暴力过滤圣像全等（含字典序）；
  象限边界点（7/8 线）一致性；退化单格累积；fail-fast。

## Out of Scope

- 不做删除/再平衡；不做浮点坐标；不做最近邻（KdTree 面）。

## Further Notes

- 与 KdTree（spec 6018）同族不同面：象限桶区域查询 vs
  轴分割最近邻。
- 里程碑：T20/50（40%）。
