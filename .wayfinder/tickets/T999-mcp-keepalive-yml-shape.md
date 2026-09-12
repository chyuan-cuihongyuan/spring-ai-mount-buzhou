---
id: T999
title: MCP keepalive yml 装配的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

spec 703 的 keepaliveInterval 只有编程面（Builder 无入口、yml 无键）——声明式部署（health timeline 实际用户）不可用。装配缝怎么落？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 25 轮 = effort #724 / spec 724 / impl 527）：三层缝全接——① `McpModule.Builder.keepalive(Duration)` fluent 面；② `fromYml` 键 `keepalive-interval`（Durations.fromMap 同款解析；缺省 = 关零变化）；③ McpModule 私有构造传 9 参注册表构造（keepaliveInterval 直通）。既有 yml（无此键）行为逐字节不变。D 会话装配轮模式（spec 504 server-breaker 同法）。
