# Spec 9006 — Boruvka 森林合并最小生成树（effort #9006，W7）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9013–W9014，impl 2359）。
> 借鉴：Boruvka 1926（MST 三经典鼻祖——并行 MST/MapReduce 骨架思想）

## Problem Statement

单分量逐步长大的病：Prim/Kruskal 序列式一轮长
一分量——**全分量每轮同步选最小出边一次性合并**，
O(log V) 轮成型，天然并行骨架。

## Solution

BoruvkaMst（core/concurrent，静态工具面）：
minimumSpanningTree(n,edges{from,to,weight}) 返回树边
序列+ totalWeight；边键全序定胜（同图同树确定）；
不连通/自环/越域 fail-fast。

## Testing Decisions

K4 经典锚（6）；等权三角确定性双跑；单点零边；
fail-fast 四面；50 随机连通图与 KruskalMst 总权全等
+异权边集全等互证。

## Out of Scope

不做森林（不连通图语义明示拒绝）；不做并行执行
（确定性序列面——并行骨架思想落地为轮次结构）；不做次小生成树。

## Further Notes

与 KruskalMst（7008）同根不同面；与 DisjointSet
（同包）配套使用。Wave 2 树结构与剖解族第一件。
