---
id: T886
title: MCP 注解聚入健康面的口径裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

Loop 1 的 toolHints() 聚合面只有 API 可读——健康快照（impl-50 范式）要不要带上它？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 19 轮 = effort #600 / spec 618 / impl 471）：

1. McpHealth details 增 `selfReportedDestructiveToolCount`（跨 server 自报 destructiveHint 工具数）。
2. **与 dangerousToolCount 分列不合并**：两数对照即「server 自报危险 vs 客户端认定危险」的差异可见（自报口径不做裁决的既有决策视觉化）；命名带 selfReported 前缀防误读。
3. 空 hints / 禁用路径零计数安全。
