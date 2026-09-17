# Spec 3018 — Tarjan 强连通分量（effort #3018，R19）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5037–T5038，impl 2019）。
> 借鉴：Tarjan 1982（index/lowlink + 栈，单遍 SCC）。

## Problem Statement

依赖图排查循环依赖时，TopologicalSorter 只报「有环」（acyclic=
false）不指认**谁在环上**——万级图里靠人肉找环成员不可行。

## Solution

`TarjanSccFinder`（core/concurrent，定容 int 宇宙）：

- `components()` 单遍 SCC（index/lowlink+显式栈）；组件按**凝聚图
  反拓扑序**（sink 侧先出——被依赖方先列）；组件内成员为栈弹出
  序（确定性）；幂等可重放；
- `hasCycle()` / `cyclicVertices()`（升序环成员——size>1 组件或
  自环单点）——环**指认**面；
- **迭代实现**（显式帧栈——2 万级深链不爆调用栈）；
- 越界/负容量 fail-fast。

## User Stories

1. 作为排障作者，「初始化循环依赖」报错直达环成员清单——免人肉
   追边。
2. 作为架构作者，凝聚图反拓扑序即分层清单——环外部分分层清晰。

## Testing Decisions

- 双点环并单组件+环成员 [0,1]；菱形 DAG 全单点无环；0→1、2→1
  反拓扑序（{1} 先出）；自环单点算环；双环相连（0↔1→2↔3）两
  组件+sink 侧先出+四环成员；2 万深链不爆栈（迭代生存证明）；
  幂等+计数读回；越界/负容量 fail-fast。

## Out of Scope

- 不做 2-SAT/缩点图构建（condensation 显式图留白）；不做加权；
  不做并行。

## Further Notes

- 与 TopologicalSorter（Kahn 序+环布尔）成对：序件+指认件。
- 里程碑：19/150。
