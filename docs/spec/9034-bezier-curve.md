# Spec 9034 — Bezier Curve 贝塞尔曲线（effort #9034，W35）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9069–W9070，impl 2387）。
> 借鉴：de Casteljau 1959/Bézier 1962（Renault CAD——CSS cubic-bezier/SVG/字体轮廓同源）

## Problem Statement

Bernstein 多项式直接展开高阶大数相消、
手工拟合无设计自由度——**de Casteljau**：
逐层线性插值，数值稳定的几何求值。

## Solution

BezierCurve（core/policy，静态纯函数面）：
pointAt(controlPoints,t)——任意维；flatten
(segments) 等参折线化；端点插值+凸包含不变量。

## Testing Decisions

线性/二次/三次 Bernstein 锚逐值；端点
插值；单点退化；30 组随机凸包含圣像；
flatten 一致性；fail-fast。

## Out of Scope

不做有理贝塞尔（NURBS 面）；不做
弧长参数化；不做 B 样条；不做度数升降
工具。

## Further Notes

与 BresenhamLine/HilbertCurve 同域
不同面。Wave 6 收束件。
