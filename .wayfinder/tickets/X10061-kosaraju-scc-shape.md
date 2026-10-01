---
id: X10061
title: X 会话 31 KosarajuScc 双 DFS 强连通分量 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

KosarajuScc（core/concurrent，静态纯函数面）：components(n,edges)——正图迭代 DFS 得完成序+逆图按完成序逆序二次 DFS 收桶；每分量升序+分量按最小顶点序；null 边/顶点越界 fail-fast；与 TarjanSccFinder 同域不同面：双 DFS 完成序 vs 单 DFS lowlink。
