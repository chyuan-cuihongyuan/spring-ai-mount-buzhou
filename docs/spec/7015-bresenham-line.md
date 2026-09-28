# Spec 7015 — BresenhamLine 直线光栅（effort #7015，U16）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7231–U7232，impl 2267）。
> 借鉴：Bresenham 1965 整数光栅思想。

## Problem Statement

离散走格的病：浮点斜率逐步累加误差（长线累积漂移）
——**误差项符号判定的整数走格面**缺失。

## Solution

`BresenhamLine`（core/policy，静态工具面）：

- 误差项逐格累积只判符号（2E 与阈值比较），无乘除
  无浮点——同参数同格序列完全确定（光栅可回放）；
- 含首尾端点；格数 = max(|dx|,|dy|)+1（经典性质）；
  每轴步进 ≤1；平局步进在反向不对称（经典变体特性
  ——测试明示不钉镜像对称，钉端点/格数/步进性质）。

## User Stories

1. 作为光栅作者，任意直线逐格走位可回放。
2. 作为审计作者，随机线性质（端点/格数/步进）逐步钉住。

## Testing Decisions

- 轴对齐/对角线/浅斜率阶梯手锚；300 随机线性质
  （端点含+格数公式+步进≤1+同参数确定性）；不钉镜像
  （经典平局特性诚实边界）。

## Out of Scope

- 不做反走样；不做粗/细粒度变体。

## Further Notes

- 与 ConvexHull（同包）同族不同面：两点间离散走格 vs
  点集外包络。
- 里程碑：U16/50（32%）。
