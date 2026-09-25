# Spec 6018 — KD-Tree 二维最近邻（effort #6018，T19）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6237–T6238，impl 2219）。
> 借鉴：kd-tree 思想（scikit-learn/图形学同源）。源码于 T18
> 对账批预入档，本轮落 spec/票/impl/README。

## Problem Statement

二维最近邻的病：每查询全点集线性扫（O(n) 放大）——**分割
剪枝索引面**缺失。

## Solution

`KdTree`（core/policy，源码已预载）：

- 交替轴中位数分割静态建树；查询沿目标侧下探、回溯查
  分割面另一侧（到轴平面距离平方 ≤ 当前最优才下探）；
- long 坐标距离平方（无浮点误差）；平局 canonical
 （距离同→x 小→y 小）；
- fail-fast：null/空/行非二维。

## User Stories

1. 作为地理作者，十万点最近邻亚毫秒——POI 底座。
2. 作为审计作者，同查询同结果——平局规则可回放。

## Testing Decisions

- 200 点×100 查询暴力扫圣像全等（含平局 canonical 规则
  双侧一致实现）；共线退化分布；重复点平局；负坐标；
  fail-fast。（初版返回值丢失深层最优与二次间接两缺陷由
  该圣像钉住改 [bestIdx,bestDist] 对返回。）

## Out of Scope

- 不做 k 近邻/半径查询（单最近邻定构）；不做动态插入；
  不做浮点坐标。

## Further Notes

- 与 QuadTree（spec 6019）同族不同面：轴分割最近邻 vs
  象限桶区域查询。
- 里程碑：T19/50（38%）。
