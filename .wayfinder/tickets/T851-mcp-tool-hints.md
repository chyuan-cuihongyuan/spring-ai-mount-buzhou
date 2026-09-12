---
id: T851
title: MCP 工具注解（readOnly/destructive hint）以观测面暴露并纳入漂移口径
type: task
status: closed
assignee: zcode-c
blocked-by:
created: 2026-09-12
---

## Question

MCP 协议（modelcontextprotocol 规范）为每个工具定义了 annotations（title / readOnlyHint / destructiveHint / idempotentHint / openWorldHint）。本仓 `buzhou-mcp` 目前只取工具名与 ToolCallback，注解整体丢弃。借鉴 modelcontextprotocol/spec 的工具注解语义，应当以什么形态把注解引入本仓？

## Resolution

**用户常设授权 AFK（2026-09-12 全程不问用户，可推翻）**

决策（F 会话第 1 轮 = effort #600 / spec 600 / impl 453）：

1. **定位 = 观测/审计面，不是护栏裁决**。既有决策（`McpClientRegistry.dangerousToolNames` Javadoc、impl-50）明确「不信任 server 自报元数据，危险性由客户端风险分类裁决」——注解不做 gate，只做目录可观测与漂移信号。
2. 形态：buzhou 自有 record `McpToolHints(title, readOnlyHint, destructiveHint, idempotentHint, openWorldHint)`（不把 SDK 类型漏进公共 API 语义承诺）；`McpConnection.toolHints()` 默认空、`McpClientRegistry.toolHints()` 聚合 ACTIVE 条目基线快照（server → tool → hints）。
3. 漂移口径扩展：`tools/list_changed` 通知里同名工具的 hints 变化（如 readOnlyHint 翻转）单独发 `mcp.tool-hints-drift` Event + `buzhou.mcp.tool-hints-drift` 指标 + WARN；名字差量事件口径不变（向后兼容）。基线无 hints（旧 server/伪连接）时跳过 hints 差量（退化不误报）。
4. 建连快照：工厂一次性 `listTools` 缓存 names + hints（此前 listToolNames 也是建连后单次快照，RPC 次数不变）。
