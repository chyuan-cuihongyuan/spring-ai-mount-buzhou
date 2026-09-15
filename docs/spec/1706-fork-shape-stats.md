# Spec 1706 — fork 树形态普查（effort #1706，R7）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2613–T2614，impl 1306）。借鉴：
> git DAG / GitHub network graph 的形态普查——fork 拓扑的深/宽/叶是治理读面。

## Problem Statement

SessionForkKeys/ForkLineageWalker 管「从哪来」（行走溯源），但没人回答
「长成什么样」：最深链多深（级联上下文膨胀风险）、最宽节点多宽（扇出热点）、
叶子多少（活跃分支面）——fork 拓扑无形态读数。

## Solution

`ForkShapeStats`（core/session，静态纯函数）：`analyze(childToParent 全集)`
→ `ShapeReport(totalSessions/rootCount/forkCount/maxDepth/maxOutdegree/
leafCount)`。父为 null/缺省 = 根；深度 = 到根链长（根 0）；出度 = 子数；
叶子 = 无子的节点。**环路诚实拒绝**（IllegalArgumentException，防死循环）。

## User Stories

1. 作为平台治理者，maxDepth=6 显形深链 fork——级联摘要链风险，考虑封顶。
2. 作为平台治理者，maxOutdegree=12 定位扇出热点会话。
3. 作为审计者，leafCount 给出当前活跃分支面。

## Implementation Decisions

- 纯读面不改 ForkLineageWalker；单遍出度统计 + 每节点上溯（环路深度上界 = 总数）。
- 空表哨兵 maxDepth=−1。

## Testing Decisions

- 空表/单根链深度/宽扇出多叶/森林多根/环拒绝 五组。

## Out of Scope

- 不做拓扑排序全序/不做最短路径；不改 fork 主流程。

## Further Notes

- 形态（本轮）+ 对账（825）+ 血缘行走（既有）= fork 治理三面。
