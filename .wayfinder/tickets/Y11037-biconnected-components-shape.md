---
id: Y11037
title: Y 会话 19 BiconnectedComponents 双连通分量 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

BiconnectedComponents（core/concurrent，静态纯函数面）：components(n,edges)——DFS disc/low+边栈（low[child]≥disc[u] 时弹至当前边成块）；边集划分承诺（Σ块边数=总边数）；null/越界 fail-fast。
