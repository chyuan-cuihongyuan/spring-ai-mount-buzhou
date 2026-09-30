# Spec 10016 — MarchingSquares 等值线（effort #10016，X17）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10033–X10034，impl 2419）。
> 借鉴：Marching Squares（Lorensen–Cline 1987 二维祖先——d3-contour 同源）

## Problem Statement

标量场等值线提取（地形/热力图/图像
描边）——解析求解不可得——栅格
2×2 位型离散连线。

## Solution

MarchingSquares（core/policy，静态纯函
数面）：contour(grid, threshold)→线段集
——四角阈值分类 16 位型、跨阈边线性
插值取点连线（分母零退 0.5）；模糊位
型 5/10 默认双段对角解（契约入档）；
网格 ≥2×2 等宽契约 fail-fast。

## Testing Decisions

单角峰单段手锚（插值 0.5 位）；全阈上/
下无线；线性斜坡等值线 0.5 位；鞍位
双段；确定性（深度递归比较——double[]
身份比较勘误入档）；参差网格 fail-fast。

## Out of Scope

不做线段拼接成环（拓扑另立）；不做 3D
Marching Cubes；不做模糊位型渐变解。

## Further Notes

Wave 3 几何族收束件：剖分/裁剪/抽稀/
样条/等值线五面全谱。
