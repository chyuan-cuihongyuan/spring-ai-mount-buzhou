# Spec 9001 — Hopcroft-Karp 二分图最大匹配（effort #9001，W2）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9003–W9004，impl 2354）。
> 借鉴：Hopcroft–Karp 1973（调度配对/任务分派同源——Kubernetes 双侧亲和配对思想）

## Problem Statement

二分匹配逐条增广的病：每轮只找一条增广路（BFS/DFS
单路 O(E)、至多 V 路）O(VE)——**阶段制：分层 BFS 定最短增广
路长度，当前弧 DFS 一轮增广出该长度的全部不相交增广路**
O(E√V)。

## Solution

HopcroftKarpMatcher（core/concurrent，静态工具面）：
matching(left,right,edges{left,right}) 返回 matchLeft（未匹配
-1）+ maxMatchingSize 便利面；匹配分布不唯一（大小承诺+取序
固定明示）；端点越域 fail-fast。

## Testing Decisions

经典手锚（4×3 大小 3+分布合法性强校验）；贪心陷阱
（先到先得钉 1、HK 必 2）；链式顶替；空图/无边退化；
50 随机图圣像（大小上界+确定性双跑）；fail-fast。

## Out of Scope

不做加权（HungarianMatcher 面）；不做一般图匹配
（blossom）；不做流显式构造（DinicMaxFlow 面）。

## Further Notes

与 HungarianMatcher（8013）同族不同面；与 DinicMaxFlow
（8006）同根（二分单位容量特例）。Wave 1 图匹配与割族第一件。
