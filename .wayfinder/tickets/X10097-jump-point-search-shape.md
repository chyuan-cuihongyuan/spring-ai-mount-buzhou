---
id: X10097
title: X 会话 49 JumpPointSearch 跳点搜索网格寻路 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

JumpPointSearch（core/concurrent，静态纯函数面）：path(walkable,start,goal)——8 向均匀格+强对角规则（斜移需双正交邻格可走）+跳点识别（直扫/斜扫遇拐强制邻格）+跳点集 A*（octile 启式）；不可达空列；null/越界/起点终点不可走 fail-fast。
