# 618 — MCP 注解聚入健康面

> 来源：F 会话第 19 轮 = effort #600（spec 600 聚合面的健康快照扩散）/ [T886](../../.wayfinder/tickets/T886-mcp-hints-health-shape.md) / [T887](../../.wayfinder/tickets/T887-mcp-hints-health-verify.md) / impl 471。

## 背景

toolHints() 聚合快照（spec 600）只有 API 面；健康快照（impl-50 范式）是运维第一入口。

## 目标

McpHealth details 增 `selfReportedDestructiveToolCount`（跨 server 自报 destructive 工具数）。

## 非目标

- 不做裁决/告警（自报口径观测）。

## 设计

与 dangerousToolCount 分列（自报 vs 客户端分类对照可见）；命名 selfReported 前缀防误读。

## 测试

2 用例：聚合计数与分列 / 空与禁用安全。

## 兼容性

details 加键纯增量。
