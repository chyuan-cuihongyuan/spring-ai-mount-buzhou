---
id: T1181
title: MCP 建连遥测读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

建连遥测与 722 并发视图/822 能力快照如何辨义分工？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 41 轮 = effort #840 / spec 840 / impl 593）：`McpConnectTelemetry`——建连本身成败/streak/lastDuration/近窗率；722 是并发占用、822 是建连后内容——三层正交；worstFirst 排序；喂点=工厂装配侧。
