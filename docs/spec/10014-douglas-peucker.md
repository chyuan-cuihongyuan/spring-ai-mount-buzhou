# Spec 10014 — DouglasPeucker 轨迹抽稀（effort #10014，X15）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10029–X10030，impl 2417）。
> 借鉴：Douglas–Peucker 抽稀（1973——Mapbox/GDAL/postgis simplify 同源）

## Problem Statement

GPS 轨迹/折线渲染点数过大——逐点均匀
抽稀丢拐点——形状感知的垂距递归抽稀。

## Solution

DouglasPeucker（core/policy，静态纯函数
面）：simplify(points, tolerance)——首末
锚定+最大垂距点分裂递归（超容差保留
分裂/收缩为两端）；端点必保留+输出为
输入子集双契约；null/点数 <2/负容差
fail-fast。

## Testing Decisions

共线收缩 2 端点；拐点保留手锚（垂距 5
保留）；50 随机正弦轨迹端点/子集契约；
容差界锚（0.3 点 0.5 容差收缩）；fail-fast
三面。

## Out of Scope

不做拓扑保持变体（Visvalingam 另立）；
不做闭合环面（首末重合域）；不做批量
容差自适应。

## Further Notes

与 SutherlandHodgman（10013）同波不同
面：抽稀 vs 裁剪。
