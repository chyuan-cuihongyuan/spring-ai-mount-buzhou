---
id: T2673
title: MCP 重连退避实效读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

McpReconnectStats 的形状怎么裁决？（spec 1736 / effort #1736 / R37）（spec 1736 验收/裁决）

## Resolution

实例面 recordAttempt(backoffMillis 负值忽略)/recordSuccess/recordGiveUp+census(successRatio −1 哨兵/maxBackoff/avgBackoff)——gRPC channelz 思想，退避涨而成功率不涨=真挂了。
