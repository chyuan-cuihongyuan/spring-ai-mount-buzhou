---
id: X10069
title: X 会话 35 DominatorTree Lengauer–Tarjan 支配树 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

DominatorTree（core/concurrent，静态纯函数面）：immediateDominators(n,edges,root)——DFS 先序编号+半支配点+并查集 eval 压缩两阶段 LT；idom[root]=root、不可达 −1；null 边/越界 fail-fast；暴力删除法独立神像交叉互证。
