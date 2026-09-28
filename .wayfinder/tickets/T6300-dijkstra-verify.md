---
id: T6298
title: T 会话 T49 Dijkstra 最短路的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6299]
created: 2026-09-28
---

## Question

T49 合同怎么逐一验绿？（spec 6049 / effort #6049 / T49）

## Resolution

**验证通过**：DijkstraShortestPathTest 五测全绿——经典
图锚值；不可达 -1；零权自环；200 随机图 vs
Bellman-Ford 圣像全等；同图双跑全等；fail-fast。
