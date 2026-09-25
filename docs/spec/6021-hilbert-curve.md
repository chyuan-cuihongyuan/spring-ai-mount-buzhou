# Spec 6021 — Hilbert Curve 希尔伯特曲线（effort #6021，T22）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6243–T6244，impl 2222）。
> 借鉴：空间填充曲线思想（PostGIS/地图瓦片同源）。源码于
> T18 对账批预入档。

## Problem Statement

二维数据一维排序的病：行序/字典序破坏空间局部性（邻点散落
全域，缓存与 IO 放大）——**局部性保持双射面**缺失。

## Solution

`HilbertCurve`（core/policy，源码已预载）：

- 标准 skew 递归：二维网格 ↔ 一维索引双射（无查表无浮点），
  相邻格子索引差有界（Z 序象限跳变被规避）；
- index/coordinate 互逆；order≤31（long 域）；
- fail-fast：阶越域、坐标/索引越界。

## User Stories

1. 作为瓦片作者，空间数据按曲线序聚簇——局部性 IO 减半。
2. 作为审计作者，全格双射可穷举验证——结构可证。

## Testing Decisions

- order4 全 256 格双射穷举（无冲突+互逆）；端点锚
 （(0,0)→0、(side−1,0)→side²−1——本变体终点锚）；order6
 单位步平均索引差 < 全域/4（局部性）；order1 形状固定；
 fail-fast。

## Out of Scope

- 不做任意矩形→索引区间分解（范围查询组装层）；不做
  高阶≥32（long 域诚实边界）。

## Further Notes

- 与 ZOrderCurve（recovery）同族不同面：希尔伯特连续走位
  vs Morton 交织跳变。
- 里程碑：T22/50（44%）。
