# Spec 9007 — Heavy-Light Decomposition 重链剖分（effort #9007，W8）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9015–W9016，impl 2360）。
> 借鉴：Heavy-Light Decomposition（树上路径问题线段树化经典前置——CP-algorithms/Link-Cut 同源思想）

## Problem Statement

树上路径逐点遍历的病：O(V)/次查询——**重链
剖分**：重儿续链轻儿新链+dfs 序链连续，任意路径
O(log V) 段区间切完。

## Solution

HeavyLightDecomposition（core/concurrent）：of(n,edges,
root) 建剖；positionOf/headOf/lca/pathSegments(u,v)；
纯剖解面（区间聚合归消费方）；迭代两遍建剖确定。

## Testing Decisions

经典 7 点树 position 双射+LCA 锚；5 组路径段覆盖
圣像；确定性；fail-fast 五面；40 随机树 LcaLifting
互证+路径覆盖数精确。

## Out of Scope

不做区间聚合（SegmentTree 消费方组合）；不做
边权树（点权面）；不做动态树（Link-Cut 面）。

## Further Notes

与 LcaLifting（7013）同域不同面；Wave 2 第二件。
