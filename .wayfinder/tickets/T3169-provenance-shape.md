---
id: T3169
title: 工具溯源索引的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

MCP 多 server 工具的归属与摘除后果怎么显形？（spec 2034 / effort #2034 / R35）

## Resolution

**双向账索引 `ToolProvenanceIndex`（buzhou-mcp provenance 新子包）**：
register 覆盖口径（重连刷新先撤后铺）+unregister 返回**独供孤儿集合**
（共供不孤儿——其余 provider 仍在）+providersOf 提供者查询+
conflictingTools 多源同名面（解析歧义源常驻显形）+serverCount/
toolCount。
