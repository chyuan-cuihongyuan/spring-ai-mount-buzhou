---
id: W9003
title: W 会话 W2 Hopcroft-Karp 二分图最大匹配 的形状裁决
type: task
status: closed
assignee: zcode-w
blocked-by: []
created: 2026-09-30
---

## Question

形状怎么定？

## Resolution

HopcroftKarpMatcher（core/concurrent）：分层 BFS+当前弧 DFS 阶段制 O(E√V)。
