---
id: T1523
title: MCP properties 装配解析统计读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 36 轮：MCP properties 装配解析统计读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 36 轮 = effort #1036 / spec 1036 / impl 788）：缺口成立——PropertiesToolSetProvider.fromServersMap（spec 04 静态清单源，启动期一次性解析）零统计：解析了几个 server、几条 binding、**bindings 清单里非 Map 的非法项被静默跳过几条**不可见（幽灵配置族——拼错形态的条目蒸发无信号）。落点 buzhou-mcp：静态进程级 servers/bindings/bindingsSkipped 三 AtomicLong + 嵌套 record `PropertiesParseStats(servers, bindings, bindingsSkipped)` + `parseStats()`/`resetForTest()`（启动期一次解析 + 测试隔离 reset；静态先例 R1/R15）。解析行为逐位不变（fail-fast 与静默跳过路径原样保留——只显形）。
