---
id: U7213
title: U 会话 U7 BellmanFord 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

负权图最短路怎么诚实？（spec 7006 / effort #7006 / U7）

## Resolution

**BellmanFord（core/concurrent）**：全边松弛 n−1 轮，
第 n 轮仍可松弛=可达负环 fail-fast；UNREACHABLE 哨兵
（-1 与负权冲突勘误）；hasNegativeCycle 超源探测。
