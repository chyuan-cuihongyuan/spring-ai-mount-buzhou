# Spec 7009 — ArticulationPoints 割点桥检测（effort #7009，U10）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7219–U7220，impl 2261）。
> 借鉴：Tarjan/Hopcroft 1973 低链接一次遍历思想。

## Problem Statement

单点故障审计的病：逐点删除+重扫连通 O(V(V+E)) 放大
——**disc/low 一次遍历割点桥面**缺失。

## Solution

`ArticulationPoints`（core/concurrent）：

- low[child]≥disc[u] 则 u 割点（子树无法绕过 u）、
  low[child]>disc[u] 则 (u,child) 为桥；根节点两孩子特判；
- 邻接按 id 升序遍历——同图同结果完全确定；重边幂等
  （重边不产生桥语义）；自环 fail-fast；
- articulationPoints（升序）/bridges（[小端,大端] 升序）。

## User Stories

1. 作为拓扑作者，识别移除即断联的关键节点/链路。
2. 作为审计作者，随机图 vs 逐点删除暴力圣像全等。

## Testing Decisions

- 路径/环/星形/双三角桥接手锚；150 随机图 vs 删除暴
  力圣像全等；桥升序确定性；fail-fast。

## Out of Scope

- 不做双连通分量分组输出；不做动态更新。

## Further Notes

- 与 TarjanSccFinder（同包）同族不同面：有向强连通分量
  vs 无向割点桥。
- 里程碑：U10/50（20%）。
