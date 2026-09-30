# Spec 10012 — DelaunayTriangulation 三角剖分（effort #10012，X13）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10025–X10026，impl 2415）。
> 借鉴：Bowyer–Watson 增量剖分（Bowyer 1981/Watson 1981——CGAL/scipy.spatial 同源）

## Problem Statement

点集三角化需空外接圆性质（最大化最小
角）——逐点增量：坏三角成腔、腔边界
重连。

## Solution

DelaunayTriangulation（core/concurrent，
静态纯函数面）：triangulate(points)→
Triangle 列表——超三角起步（非整偏移
破格点共圆巧合）、逐点插入（外接圆判
定严格内+有向腔边界保 CCW 绕向）、虚
点滤除；重复点/全共线/点数 <3 fail-fast。

## Testing Decisions

方格手锚（4 点 2 三角全点覆盖）；空圆
性质随机 20 组逐点核验；随机点集全点
覆盖；退化契约 fail-fast 四面。

## Out of Scope

不做约束边/带洞剖分（CDT 另立）；不做
Delaunay 细化（Ruppert 另立）；共点共
圆退化域 fail-fast 而非符号扰动。

## Further Notes

开发勘误两处入档（无向边界编码丢绕向
→有向腔边界；整数格超三角与点阵精确
共圆→非整偏移）；Wave 3 几何族首件。
