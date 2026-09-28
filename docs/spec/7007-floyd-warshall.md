# Spec 7007 — FloydWarshall 全对最短路（effort #7007，U8）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7215–U7216，impl 2259）。
> 借鉴：Floyd 1962 / Warshall 1962 动态规划闭包思想。

## Problem Statement

全对最短路的病：单源跑 n 遍 Bellman-Ford O(V²E)（稠密
图放大）——**中转点逐层闭包 O(V³) 一次全对面**缺失。

## Solution

`FloydWarshall`（core/concurrent）：

- d[i][j]=min(d[i][j], d[i][k]+d[k][j]) 三重循环闭包；
  负权合法；**对角负值 = 负环 fail-fast**（诚实拒绝）；
- 多边取最小折叠；不可达 = UNREACHABLE 哨兵（同
  BellmanFord 勘误——-1 与负权真实距离冲突）；
- addEdge/allDistances（返回副本）/nodeCount/edgeCount。

## User Stories

1. 作为网络作者，一次闭包全对距离。
2. 作为审计作者，随机图逐行 vs Dijkstra 圣像全等。

## Testing Decisions

- 手锚闭包逐值；60 随机图全行 vs Dijkstra；负权与负环
  fail-fast；多边折叠；fail-fast。

## Out of Scope

- 不做路径矩阵还原；不做位并行闭包。

## Further Notes

- 与 BellmanFord（7006）同族不同面：单源全松弛 vs 全对
  中转闭包。
- 里程碑：U8/50（16%）。
