---
id: T5025
title: Q 会话 R13 拓扑排序器的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

依赖序怎么确定性解析且环显形？（spec 3012 / effort #3012 / R13）

## Resolution

**TopologicalSorter（core/concurrent）**：Kahn 入度归零 + 最小下标
优先队列（字典序最小拓扑序——确定性可复算）+ SortResult(order,
acyclic) 环诚实（有环给环外前缀不臆造全序）+ 自环即环 + sort 幂等
可重放 + 越界 fail-fast。
