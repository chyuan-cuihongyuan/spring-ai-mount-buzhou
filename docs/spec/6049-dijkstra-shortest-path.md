# Spec 6049 — Dijkstra 最短路（effort #6049，T49）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6299–T6298，impl 2250）。
> 借鉴：Dijkstra 1959 贪心已决集思想（Neo4j GDS/导航引擎同源）。

## Problem Statement

非负权单源最短路的病：无权 BFS（浪费权重信息）与
每步全扫取最小 O(V²)（稀疏图浪费）——**贪心已决集扩张 +
堆加速面**缺失。

## Solution

`DijkstraShortestPath`（core/concurrent，与 TopologicalSorter/
TarjanSccFinder 同包先例）：

- 每步取未决 dist 最小者定影（已决集 dist 永不回改），
  松弛沿出边推进；堆直接复用同包 IndexedHeap（
  updatePriority decrease-key 一等公民，O((V+E) log V)）；
- 负权边破坏贪心前提 fail-fast；不可达 = -1（诚实
  缺省）；零权/自环无害；堆序确定性（同 dist 按
  id 小者先出）——同图同结果；
- addEdge/distancesFrom/nodeCount/edgeCount 读数；越域/
  负权/非正节点数 fail-fast。

## User Stories

1. 作为路由作者，非负权图单源最短路一次求全场。
2. 作为审计作者，200 随机图 vs Bellman-Ford 圣像逐步全等
   ——正确性可证。

## Testing Decisions

- 经典教科书图锚值逐值钉住；不可达 -1；零权自环
  无害；200 种子化随机图 vs 测内 Bellman-Ford 圣像全等；
  同图双跑全等；fail-fast 四路。

## Out of Scope

- 不做路径还原（只交距离面）；不做多源/终点早退；
  负权图明确拒绝（不降级 Bellman-Ford）。

## Further Notes

- 与 IndexedHeap（6026）同族不同面：图算法原语 vs 原语的
  经典消费方；与 TopologicalSorter 不同面：DAG 序 vs 边权
  最优化。
- 里程碑：T49/50（98%）。
