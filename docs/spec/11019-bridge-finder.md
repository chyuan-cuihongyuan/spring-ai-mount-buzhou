# Spec 11019 — BridgeFinder 桥检测（effort #11019，Y20）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11039–Y11040，impl 2472）。
> 借鉴：Tarjan 1974 思想——网络可靠性同源（BiconnectedComponents 已占异面：块划分 vs 割边列表）

## Problem Statement

无向图割边——删除后图不连通的关键链路面。

## Solution

BridgeFinder（core/concurrent）：bridges(int,int[][])——Tarjan disc/low：树边 (u,child) 满足 low[child]>disc[u] 即桥；重边使父边不构成桥（第二平行边成回边）。

## Testing Decisions

链图全桥+三角零桥+双三角共点单桥+重边非桥+随机 20 图与暴力删边 BFS 连通性交叉互证圣像+确定性+fail-fast 五面。

## Out of Scope

不做割边树/2-边连通分量输出（消费方自组）；不做有向桥面；不做动态更新。

## Further Notes

与 BiconnectedComponents（11018）同域不同面：割边列表 vs 块划分；Wave 4 图结构族第二件。
