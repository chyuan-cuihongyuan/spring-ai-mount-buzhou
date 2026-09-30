# Spec 9003 — Stoer-Wagner 全局最小割（effort #9003，W4）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9007–W9008，impl 2356）。
> 借鉴：Stoer–Wagner 1997（网络分区容错/图聚类同源——GRAPHVIZ 社区分割思想）

## Problem Statement

无向全局割逐子集枚举的病：2^V 子集扫描指数
爆炸——**MAO 阶段制：每阶段按最紧连接入序，末位候选割，
收缩两后下一阶段**，n−1 阶段必取到全局最小。

## Solution

StoerWagnerMinCut（core/concurrent，静态工具面）：
minCut(n,edges{u,v,w}) 返回割值；无向合并平行边；
割值唯一（割侧分布不唯一明示）；同权最低编号取序固定；
自环/越域/非正权 fail-fast。

## Testing Decisions

K4 经典锚（3）；双三角桥（2）；平行边合并；两点
无边（0）；路径弱独占（1）；40 随机图割值≤总权和+确定性；
fail-fast。

## Out of Scope

不做割侧提取（割值承诺）；不做有向 s-t 割
（最大流面）；不做 k 割（k>2）。

## Further Notes

与 EdmondsKarpMaxFlow/DinicMaxFlow 同根不同面
（max-flow min-cut 另一半）。Wave 1 第三件。
