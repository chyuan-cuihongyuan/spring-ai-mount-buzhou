---
id: X10067
title: X 会话 34 KShortestPaths Yen 偏离 K 最短路 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

KShortestPaths（core/concurrent，静态纯函数面）：kShortest(n,edges,s,t,k)——Dijkstra 首路+逐 spur 节点禁边禁点偏离+候选池择小（权重并列按路径字典序确定性口径）；Path(totalWeight,nodes) record；不足 k 条如数返回；负权重/源汇同点/越界 fail-fast。
