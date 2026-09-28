# Spec 7012 — ConvexHull 凸包（effort #7012，U13）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7225–U7226，impl 2264）。
> 借鉴：Andrew 1979 monotone chain 思想。

## Problem Statement

外包络的病：两两枚举判断 O(n³)（点数放大不可承受）
——**排序后上下链双扫 O(n log n) 面**缺失。

## Solution

`ConvexHull`（core/policy，静态工具面）：

- 严格凸包（共线边界点不保留）；退化：<3 点/全共线
  返回端点序列（诚实降级）；重复点幂等；
- 输出逆时针自最左最低点起（同点集同序列完全确定）；
- 叉积 long 域——坐标 ±10^9 安全域 fail-fast（乘积溢出
  诚实拒绝，不做浮点）。

## User Stories

1. 作为地理围栏作者，点集最小外包凸多边形。
2. 作为审计作者，随机点集凸性+包含性质逐步钉住。

## Testing Decisions

- 方形/菱形手锚（CCW 逐序）；共线段退化；200 随机点集
  凸性（相邻叉积>0）+全点包含性质；幂等；坐标越域 fail-fast。

## Out of Scope

- 不做动态凸包；不做共线保留变体。

## Further Notes

- 与 KdTree（同包）同族不同面：最近邻查询面 vs 外壳包络面。
- 里程碑：U13/50（26%）。
