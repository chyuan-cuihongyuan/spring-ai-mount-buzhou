# Spec 10032 — PushRelabelMaxFlow 推重标最大流（effort #10032，X33）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10065–X10066，impl 2435）。
> 借鉴：Goldberg–Tarjan 1988 思想——LEMON/Boost Graph 同源（EdmondsKarp/Dinic 已占异面：增广路）

## Problem Statement

增广路族（EdmondsKarp/Dinic 已占）之外的最大流第三法——预流推进的局部高度面。

## Solution

PushRelabelMaxFlow（core/concurrent）：maxFlow(n,edges,s,t)——源点 preflow 满推（height[s]=n）+FIFO 活跃队列逐点 discharge：残量正且 h[u]=h[v]+1 推进、否则重标为最小可推邻高度+1；流值=excess[t]；long 容量域残量网络邻接头插法与 Dinic 同构。

## Testing Decisions

CLRS 手锚流值与 DinicMaxFlow 交叉互证+30 随机图（含反平行/平行边）流值相等圣像+零容量断边连通面+确定性+fail-fast 五面。

## Out of Scope

不做 highest-label/间隙启发/全局重标变体（论文变体另立）；不做流分布分解输出（流值唯一承诺沿 Dinic 口径）。

## Further Notes

与 EdmondsKarpMaxFlow/DinicMaxFlow（已占）同域不同面：预流推进局部高度 vs 分层增广路；Wave 6 图结构进阶族第三件。
