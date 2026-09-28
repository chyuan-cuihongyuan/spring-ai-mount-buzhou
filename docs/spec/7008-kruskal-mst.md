# Spec 7008 — KruskalMst 最小生成树（effort #7008，U9）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7217–U7218，impl 2260）。
> 借鉴：Kruskal 1956 贪心 + 并查集判环思想。

## Problem Statement

最小连接的病：全枚举生成树指数爆炸——**边权升序扫描
+ 并查集判环的贪心面**缺失。

## Solution

`KruskalMst`（core/concurrent）：

- 边排序键 (weight,from,to) 全序——同图同树完全确定；
  并查集判环（复用同包 DisjointSet），n−1 边即止；
- minimumSpanningTree（采纳序）/totalWeight；不连通
  fail-fast（生成树不存在——森林语义明示不做）；自环
  fail-fast（无环图不变量）。

## User Stories

1. 作为网络作者，最小总权连通骨架。
2. 作为审计作者，随机连通图 vs Prim 圣像总权全等。

## Testing Decisions

- 经典图总权 15 逐值+首边 [1,0,2] 钉住；同权边双实例
  采纳序全等；100 随机连通图（先生成树保证连通）vs 测内
  Prim 圣像总权全等；不连通/自环 fail-fast。

## Out of Scope

- 不做次小生成树；不做增量加边维护。

## Further Notes

- 与 DisjointSet（同包）同族不同面：判环原语 vs 原语的
  经典消费方；与 DijkstraShortestPath 不同面：全树最小
  连接 vs 单源最短路。
- 里程碑：U9/50（18%）。
