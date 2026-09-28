---
id: T6297
title: T 会话 T49 Dijkstra 最短路的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-28
---

## Question

非负权单源最短路怎么堆加速？（spec 6049 / effort #6049 / T49）

## Resolution

**DijkstraShortestPath（core/concurrent）**：贪心已决集
扩张+同包 IndexedHeap decrease-key 松弛（O((V+E) log V)）；
负权 fail-fast；不可达 -1；堆序确定。
