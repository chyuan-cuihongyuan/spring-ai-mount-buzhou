# Spec 7014 — PointInPolygon 点在多边形（effort #7014，U15）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7229–U7230，impl 2266）。
> 借鉴：射线法 crossing number（GIS/图形学同源思想）。

## Problem Statement

点隶属判定的病：逐边角度求和浮点累计误差/对每查询
重量化——**整数半开穿越计数面**缺失。

## Solution

`PointInPolygon`（core/policy，静态工具面）：

- 水平射线穿越计数奇内偶外；穿越判定整数不等式
  （s,t 符号对——无除法无浮点）；半开规则处理顶点对齐
  （恰一端严格在射线上方才计穿越——标准稳定性技巧）；
- 边界=内（GIS「多边形含其边界」约定，onSegment 前置
  短路）；简单多边形假设（自相交语义未定义——明示）；
  <3 顶点 fail-fast。

## User Stories

1. 作为围栏作者，整数坐标域无浮点误差的隶属判定。
2. 作为审计作者，凹多边形手锚逐点钉住。

## Testing Decisions

- 方形内外+全边界；凹口排除（V 顶上方为外）逐点；
  三角形顶点对齐半开规则；fail-fast。

## Out of Scope

- 不做自相交多边形；不做点集批量查询优化。

## Further Notes

- 与 KdTree（同包）同族不同面：最近邻面 vs 隶属面。
- 里程碑：U15/50（30%）。
