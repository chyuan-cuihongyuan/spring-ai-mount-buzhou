---
id: U7214
title: U 会话 U7 BellmanFord 的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7213]
created: 2026-09-29
---

## Question

U7 合同怎么逐一验绿？（spec 7006 / effort #7006 / U7）

## Resolution

**验证通过**：六测全绿——负权手锚；哨兵；可达负环
fail-fast；全局负环；100 随机图 vs Dijkstra 全等。
