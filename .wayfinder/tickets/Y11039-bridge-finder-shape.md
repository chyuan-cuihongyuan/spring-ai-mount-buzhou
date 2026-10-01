---
id: Y11039
title: Y 会话 20 BridgeFinder 桥检测 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

BridgeFinder（core/concurrent，静态纯函数面）：bridges(n,edges)——DFS disc/low 树边 low[child]>disc[u] 判桥；重边非桥口径；暴力删边独立神像交叉互证；null/越界 fail-fast。
