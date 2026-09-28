---
id: U7215
title: U 会话 U8 FloydWarshall 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

全对最短路怎么一次闭包？（spec 7007 / effort #7007 / U8）

## Resolution

**FloydWarshall（core/concurrent）**：中转点闭包 O(V³)；
负权合法对角负值=负环 fail-fast；多边折叠；UNREACHABLE 哨兵。
