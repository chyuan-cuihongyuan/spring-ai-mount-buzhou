# Spec 9048 — A* Search 启发式最短路（effort #9048，W49）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9097–W9098，impl 2401）。
> 借鉴：A*（Hart-Nilsson-Raphael 1968——游戏寻路/导航同源的启发式决定论证明）

## Problem Statement

Dijkstra 无向四撒均匀探索全圆、贪心最佳
优先不可采纳可绕远——A*：f=g+h
聚焦朝目标前沿。

## Solution

AStarSearch（core/concurrent，静态纯函数面）：
shortestPath(n,edges,source,target,heuristic)——
无向边；陈旧条目门；平局确定取序。

## Testing Decisions

菱形/网格曼哈顿锚；h=0 退化 Dijkstra；
60 随机图（Dijkstra 双向加边对齐口径）
逐图全等；fail-fast。

## Out of Scope

不做路径回读（距离面）；不做双向 A*；
不做动态重规划（D* Lite 另立）；
不可达 fail-fast 而非 ∞ 返回（契约明示）。

## Further Notes

与 DijkstraShortestPath（同包）同根不同面：
h≡0 恰退化；有向/无向口径差异入档。
W49 独件（Wave 9）。
