# Spec 10033 — KShortestPaths Yen 偏离 K 最短路（effort #10033，X34）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10067–X10068，impl 2436）。
> 借鉴：Yen 1971 思想——networkx/PG Routing 同源（DijkstraShortestPath 已占异面：单最短路）

## Problem Statement

单最短路（Dijkstra 已占）之外的次优备选面——绕行/备选路由的 K 最短路径列。

## Solution

KShortestPaths（core/concurrent）：kShortest(n,edges,s,t,k)——内嵌确定性 Dijkstra（(dist,node) 堆并列按点号）求首路；Yen 偏离：对已取路径逐 spur 节点禁共享边+禁根点、spur 到汇子路拼根成候选；候选池择小（权重并列按节点序字典序）；Path(totalWeight,nodes) 不可变 record 升序列。

## Testing Decisions

菱形三路手锚（权重升序+节点序手锚）+k 超量如数返回+30 随机图首路与 DijkstraShortestPath 值相等+候选路径合法性与权重和自洽圣像+确定性+fail-fast 五面。

## Out of Scope

不做简单路径约束外的环游枚举；不做 Eppstein 渐近变体（论文变体另立）；不做负权重域（Dijkstra 域沿袭）。

## Further Notes

与 DijkstraShortestPath（已占）同域不同面：K 条备选偏离列 vs 单最短路；Wave 6 图结构进阶族第四件。
