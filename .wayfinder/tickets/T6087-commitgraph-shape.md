---
id: T6087
title: R 会话 R44 提交图世代号的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-24
---

## Question

依赖图血缘判定怎么 O(1) 剪枝不靠时钟？（spec 4043 /
effort #4043 / R44）

## Resolution

**CommitGraph（core/policy）**：Git commit-graph 思想——
世代号 gen=max(父)+1（根=1）O(1) 读；isAncestor 先 gen 剪枝
（gen(a)≥gen(b) 确定非）再有界 BFS 精确判定；拓扑序注册
DAG-by-construction fail-fast。
