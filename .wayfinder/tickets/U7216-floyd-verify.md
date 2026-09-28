---
id: U7216
title: U 会话 U8 FloydWarshall 的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7215]
created: 2026-09-29
---

## Question

U8 合同怎么逐一验绿？（spec 7007 / effort #7007 / U8）

## Resolution

**验证通过**：四测全绿——手锚闭包；60 随机图逐行 vs
Dijkstra；负权/负环；多边折叠；fail-fast。
