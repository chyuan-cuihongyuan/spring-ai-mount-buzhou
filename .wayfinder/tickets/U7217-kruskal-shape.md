---
id: U7217
title: U 会话 U9 KruskalMst 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

最小连接怎么贪心+判环？（spec 7008 / effort #7008 / U9）

## Resolution

**KruskalMst（core/concurrent）**：(w,from,to) 全序
排序+DisjointSet 判环 n−1 边即止；不连通/自环 fail-fast。
