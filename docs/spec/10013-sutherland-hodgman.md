# Spec 10013 — SutherlandHodgman 多边形裁剪（effort #10013，X14）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10027–X10028，impl 2416）。
> 借鉴：Sutherland–Hodgman 裁剪（1974——OpenGL scissor/游戏引擎同源）

## Problem Statement

多边形对凸窗裁剪——视口剔除/地图瓦
片/渲染管线基元——逐凸边扫割重连
的标准直接法。

## Solution

SutherlandHodgman（core/policy，静态纯
函数面）：clip(polygon, clipWindow)——
每边按内侧/外侧四情形保留/交点插入
级联缩腔；窗凸且 CCW 契约（凸性/绕向
fail-fast——凹窗 Weiler–Atherton 另立）；
完全在外返回空表（契约明示）。

## Testing Decisions

内含多边恒等；方窗边界交点手锚（1.5
出界三角裁出 4 顶点）；全出空表；越界
大矩全顶点入窗；凹窗/ CW 窗/顶点不足
fail-fast 六面。

## Out of Scope

不做凹窗（Weiler–Atherton 另立）；不做
多窗布尔并交（Greiner–Hormann 另立）；
退化共线窗边按容差内视为内侧。

## Further Notes

Wave 3 几何族第二件；与 Delaunay（10012
——concurrent）同波异包：剖分 vs 裁剪。
