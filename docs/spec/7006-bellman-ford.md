# Spec 7006 — BellmanFord 负权最短路（effort #7006，U7）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7213–U7214，impl 2258）。
> 借鉴：Bellman 1958 / Ford 1956 全松弛思想。

## Problem Statement

负权图的病：错用 Dijkstra（贪心定影被负边回改，结果错误
且无声）——**全边松弛 n−1 轮 + 负环检测面**缺失。

## Solution

`BellmanFord`（core/concurrent）：

- 全边松弛 n−1 轮；第 n 轮仍可松弛 = **可达负环
  fail-fast**（距离无定义，诚实拒绝而非吐振荡值）；
- 不可达 = UNREACHABLE 哨兵（Long.MIN_VALUE/2——负权下
  -1 与真实距离冲突，勘误入档：初版 -1 哨兵在真实距离
  −1 时误判不可达，随机交叉圣像钉住修正）；
- hasNegativeCycle（虚拟超源全零——与单源可达性无关）；
- 越域 fail-fast。

## User Stories

1. 作为路由作者，负权边图正确最短路+负环显式拒绝。
2. 作为审计作者，非负随机图 vs Dijkstra 圣像逐步全等。

## Testing Decisions

- 负权手锚逐值；不可达哨兵；可达负环 fail-fast；不可达
  负环全局探测；100 非负随机图 vs Dijkstra 全等；fail-fast。

## Out of Scope

- 不做路径还原；不做 SPFA 队列优化（O(VE) 诚实边界）。

## Further Notes

- 与 DijkstraShortestPath（6049）同族不同面：非负贪心
  O((V+E)logV) vs 负权全松弛 O(VE)+负环检测。
- 里程碑：U7/50（14%）。
