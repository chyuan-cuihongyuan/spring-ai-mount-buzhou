# 1036 — MCP properties 装配解析统计读面

> 来源：J 会话第 36 轮 = effort #1036（[T1523](../../.wayfinder/tickets/T1523-mcp-parse-stats-shape.md) / [T1524](../../.wayfinder/tickets/T1524-mcp-parse-stats-verify.md) / impl 788）。与 R3/R16 同族：**配置静默跳过显形**（pnpm/yarn 清单解析统计思想）。

## Problem Statement

PropertiesToolSetProvider.fromServersMap（spec 04 静态清单源）启动期一次性解析 `buzhou.mcp.servers.*`：server 数、binding 数、**bindings 清单里非 Map 非法项的静默跳过**均无统计——形态拼错的 binding 条目蒸发无信号，MCP 装配面覆盖不可见。

## 目标

- `PropertiesToolSetProvider` 增量（buzhou-mcp，静态进程级——启动期一次解析后只读）：`servers` / `bindings` / `bindingsSkipped` 三 AtomicLong。
- 嵌套 record `PropertiesParseStats(long servers, long bindings, long bindingsSkipped)` + 静态 `parseStats()` 快照 + `resetForTest()`。
- 解析行为逐位不变：transport 非法仍 fail-fast 抛 BuzhouConfigurationException；bindings 非 Map 项仍静默跳过（本轮只显形）。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 非法 binding 项升级 fail-fast（语义变化——本轮只显形不拦截）。
- DbToolSetProvider / 动态源统计（DB 侧另有审计面）。
