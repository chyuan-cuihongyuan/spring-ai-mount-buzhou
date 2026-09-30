# Spec 9004 — Bron-Kerbosch 极大团枚举（effort #9004，W5）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9009–W9010，impl 2357）。
> 借鉴：Bron–Kerbosch 1973 + Tomita pivot（社交网络社区发现/图分析同源——networkx cliques 同款）

## Problem Statement

全子集扫描判极大的病：2^n 子集×逐子集团判定
指数爆炸——**三集递归 R/P/X + pivot 剪枝**：只递归
P∖N(pivot)，最坏 3^(n/3) 上界、实战分支最省。

## Solution

BronKerboschCliques（core/concurrent，静态工具面）：
maximalCliques(n,edges{u,v}) 返回全部极大团（每团升序、
枚举序确定）；重复边归一；自环拒绝/越域 fail-fast。

## Testing Decisions

K4 唯一团；C5 五环 5 边团；空图单点团；n=0 零团；
三角垂耳；30 随机图逐团团性+极大性圣像；内容确定性双跑；
fail-fast。

## Out of Scope

不做最大团（规模最优化的 NP-hard 面—— clique
number 只由枚举可推）；不做补图独立集显式面；不做加权团。

## Further Notes

与 TarjanSccFinder（7020）同域不同面；与
HopcroftKarpMatcher（9001）对偶。Wave 1 收束件。
