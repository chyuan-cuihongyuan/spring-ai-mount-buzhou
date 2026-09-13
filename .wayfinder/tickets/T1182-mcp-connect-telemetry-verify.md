---
id: T1182
title: MCP 建连遥测读数验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1181]
created: 2026-09-13
---

## Question

计数/streak/率/排序如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 41 轮 = effort #840）：McpConnectTelemetryTest 4 例——0.5 率+streak 清零+lastDuration/未知时长保留/worstFirst 失败降序/封顶+脏入参。
