---
id: T2674
title: MCP 重连退避实效读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2673
created: 2026-09-15
---

## Question

McpReconnectStats 怎么验证？（spec 1736 验收/裁决）

## Resolution

McpReconnectStatsTest：三笔退避账+2/3 成功率/负值忽略/空哨兵。
