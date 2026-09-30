# Spec 9008 — Centroid Decomposition 重心剖分（effort #9008，W9）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9017–W9018，impl 2361）。
> 借鉴：Centroid Decomposition 点分治（CP-algorithms 树上路径统计分治经典——顶点覆盖路径统计思想）

## Problem Statement

整树逐对路径扫描的病：O(V²) 起步——**重心剖分**：
每块取重心递归分块，树高 O(log V)，路径统计过
重心分治恰一次。

## Solution

CentroidDecomposition（core/concurrent）：of(n,edges)
建剖；rootCentroid/parentInCentroidTree/componentSizeOf/
childrenInCentroidTree；并列重心取最低编号（确定）。

## Testing Decisions

星/链/单点锚；经典树三不变量（父块>子块/恰一根/
树高松弛界）；40 随机树不变量圣像；确定性；fail-fast。

## Out of Scope

不做距离统计（消费方组合面）；不做边权；
不做动态树。

## Further Notes

与 HeavyLightDecomposition（9007）同域不同面。
Wave 2 第三件。
