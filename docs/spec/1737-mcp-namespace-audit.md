# Spec 1737 — MCP 命名空间冲突普查（effort #1737，R38）（effort #1737，R38）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2675–T2676，impl 1337，impl npm scope 冲突）。借鉴：多 MCP 服务端暴露同名工具时路由有歧义——「哪些工具名被多服务端占用」无普查，装配冲突后置成运行期事故。

## Problem Statement

`McpNamespaceAudit`（mcp，实例面 synchronized）：register(server, tool) 逐工具登记（缺名归 _anonymous_/_anonymous_tool_）+census（tools/collidingTools 多服务端占用/maxServersPerTool）+serversOf(tool) 占用集合。与 McpDirectoryDiff 互补。纯读面 opt-in。

## Solution

作为装配审计者，collidingTools>0 → 装配期即告警而非运行期歧义。

## User Stories

1. 17370
2. 17371
3. 17372

## Implementation Decisions

- 17373

## Testing Decisions

- 17374

## Out of Scope

- 17375

## Further Notes

- 17376
