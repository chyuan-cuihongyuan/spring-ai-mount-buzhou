---
id: V8017
title: V 会话 V9 BipartiteChecker 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-29
---

## Question

冲突双分怎么线性判定？（spec 8008 / effort #8008 / V9）

## Resolution

**BipartiteChecker（core/concurrent）**：BFS 交替染色撞色即
非二分；isBipartite/sides 双面（非二分 sides 空诚实缺省）；
自环 fail-fast；孤立节点归 0 侧 canonical。
