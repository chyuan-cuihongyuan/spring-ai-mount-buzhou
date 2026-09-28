---
id: U7229
title: U 会话 U15 PointInPolygon 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

点隶属怎么整数无浮点？（spec 7014 / effort #7014 / U15）

## Resolution

**PointInPolygon（core/policy）**：射线穿越奇内偶外，
整数符号对判定；边界=内；半开顶点规则；简单多边形假设。
