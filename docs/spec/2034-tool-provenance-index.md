# Spec 2034 — 工具溯源索引（effort #2034，R35）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3169–T3170，impl 1585）。
> 借鉴：MCP 多 server 工具记账——tool ↔ server 双向账与摘除后果显形。

## Problem Statement

MCP 多 server 工具聚合后：server 摘除（断连/下线）哪些工具将失源
（孤儿——调用方无从路由）事前无人知；多 server 同名工具（解析歧义
 * 源）无常驻显形。目录差异报告（706）看两次快照间变化，静态归属
 * 与摘除后果缺一面。

## Solution

`ToolProvenanceIndex`（buzhou-mcp provenance 新子包，synchronized
小临界区）：

- `register(serverId, toolNames)`：铺双向账（重复注册**覆盖**——重连
  刷新口径：先撤旧账再铺新）；
- `unregister(serverId)`：返回其**独供**工具集合（孤儿——无其他
  provider 即刻显形）；共供工具不孤儿（其余 provider 仍在）；
- `providersOf(tool)`：提供者集合（空 = 无源）；`conflictingTools()`：
  多源同名工具面（provider 数字典序）；serverCount / toolCount。

## User Stories

1. 作为运维，摘 server 前先看孤儿清单——影响面事前可见。
2. 作为集成作者，conflictingTools 常驻显形——同名工具歧义源可治
  （改名或显式路由）。

## Testing Decisions

- 双向账；摘独供者孤儿双工具 + 旁源不动；共供不孤儿/最后 provider
  走才孤儿；覆盖重注册（旧工具失源/不重复注册）；三源冲突面；未知
  server 摘除空；畸形七型 fail-fast。

## Out of Scope

- 不做路由决策（歧义解析归调用方）；不接 MCP 客户端注册表联动
  （接线归后续轮）。

## Further Notes

- 与 MCP 工具目录差异报告（706）互补：快照间变化 vs 任一时刻静态
  归属与摘除后果。
