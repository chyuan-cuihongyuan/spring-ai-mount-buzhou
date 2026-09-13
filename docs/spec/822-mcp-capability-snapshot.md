# 822 — MCP 能力协商快照

> 来源：H 会话第 23 轮 = effort #822 / [T1145](../../.wayfinder/tickets/T1145-mcp-capability-snapshot.md) / [T1146](../../.wayfinder/tickets/T1146-mcp-capability-snapshot-verify.md) / impl 575。
> 借鉴：LSP initialize capabilities（≈11K star）。

## Problem

MCP server 建连时「声明了什么」没有单点存档形状：706 需要两份快照才能 diff——基线快照本身（工具数/hint 覆盖/只读破坏计数/名册指纹）缺位。

## Solution

`McpCapabilitySnapshot`（mcp，纯函数）：

- **采集**：of(server, connection, atMs)——listToolNames 排序名册（空则降级 toolCallbacks 定义名提取）；toolHints 计数（覆盖数/readOnly/destructive）。
- **指纹**：排序名册 join「,」（确定性；乱序入归一出；无加密语义）。
- **降级**：seam 异常逐路吞（names→callbacks→空真）——采集不炸；null connection fail-fast。

## 兼容性

纯新增静态工具；McpConnection 接口零变更（作为 706 的基线输入形状）。

## 诚实边界

不做 initialize 握手全量记录（SDK seam 不暴露——边界诚实）；指纹非加密；hint 观测不裁决（600 决策延续）。
