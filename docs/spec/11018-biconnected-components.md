# Spec 11018 — BiconnectedComponents 双连通分量（effort #11018，Y19）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11037–Y11038，impl 2471）。
> 借鉴：Tarjan 1972 思想——Hopcroft–Tarjan 同源（KosarajuScc 已占异面：有向 SCC vs 无向 BCC）

## Problem Statement

无向图的 2-边连通块——割点删除后连通性的块划分面。

## Solution

BiconnectedComponents（core/concurrent）：components(int,int[][])——Tarjan disc/low DFS+显式边栈：回边压栈、low[child]≥disc[u] 触发弹栈至 (u,child) 成块；孤立顶点不产出（无边即无块）。

## Testing Decisions

三角单块手锚+双三角共点两块手锚+链图 n−1 逐边块+边集划分圣像（Σ=m 不重不漏，随机 20 图）+确定性+fail-fast 五面。

## Out of Scope

不做顶点双连通输出（边块口径）；不做割点列表（ArticulationPoints 已占）；不做增量维护面。

## Further Notes

与 KosarajuScc（10030）同域不同面：有向 SCC vs 无向 BCC；Wave 4 图结构族首件。
