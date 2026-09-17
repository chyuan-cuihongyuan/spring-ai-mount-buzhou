# Spec 3012 — 拓扑排序器（effort #3012，R13）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5025–T5026，impl 2013）。
> 借鉴：Kahn 算法（入度归零入队）+ 字典序最小贪心。

## Problem Statement

依赖序解析（工具注册依赖 / hook 装配次序 / 模型回退链）手工排序
易错且环依赖（A→B→A）常被静默吞掉或死循环——需要一个确定性、
环诚实报告的排序原语。

## Solution

`TopologicalSorter`（core/concurrent，定容 int 宇宙）：

- `addEdge(from, to)` 有向边（to 依赖 from——from 先行）；
- `sort()` Kahn：入度归零入**最小下标优先队列**（字典序最小拓扑
  序——确定性可复算，非入队序依赖）；
- **环诚实**：`SortResult(order, acyclic)`——有环时 acyclic=false
  且 order 为环外已排前缀（不臆造全序）；自环即环；
- sort 幂等可重放（不改图）；越界/负容量 fail-fast。

## User Stories

1. 作为装配作者，依赖链一次排序——每条边前驱先于后继。
2. 作为排障作者，环依赖显形（acyclic=false + 环外前缀可读）。

## Testing Decisions

- CLRS 经典六点 DAG 手算字典序最小序 [4,5,0,2,3,1]；每边前驱
  先于后继逐对断言；三角环+孤立点 → 前缀 [3] / acyclic=false；
  自环 → 空序 + false；无边全顶点升序；计数读回；越界/负容量
  fail-fast；随机 DAG 20 次试验（20 点 p=0.2 前向边）每边次序 +
  acyclic 恒成立。

## Out of Scope

- 不做 Tarjan SCC 环成员定位（后续轮候选）；不做泛型载荷
  （int 宇宙外层 Map 映射）；不做并行排序。

## Further Notes

- 与 CriticalPathLength（内部 Kahn+EF，假定无环带权）互补：本件
  无权 + 环诚实，是依赖解析的地基原语。
- 里程碑：13/150。
