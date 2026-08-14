# Spec 18 — MCP 工具集漂移检测（mechanism）

> effort #5（T86 / impl-61）。此前 refresh 仅由本地配置变更触发（replaceAll diff），server 端
> 工具变更完全不可见。

## 协议订阅（事件驱动，无轮询）

- **SDK 能力**：MCP Java SDK 2.0.0 `McpClient.SyncSpec.toolsChangeConsumer(Consumer<List<Tool>>)`
  ——协议 `tools/list_changed` 通知直达注册表。<b>不做轮询兜底</b>（诚实边界：连接断开期间的
  变更在下一次重连 / 配置 refresh 重建时自然吸收）。
- **接线**：`McpConnectionFactory` 增 default `connect(spec, toolsChangedListener)`（默认忽略
  listener，兼容既有实现与测试 fake）；`SpringAiMcpConnectionFactory` 覆写挂 consumer。
  `McpConnection` 增 default `listToolNames()`（SDK 原始口径，建连基线快照）。

## 基线差量

- 注册表 Entry 持工具名基线集（建连时 `listToolNames()`）；通知到达 → 与基线差量
  （added / removed；<b>不比 description 变更</b>——SDK 只给新列表，desc 差量噪音大）。
- 非空差量：`mcp.tools-drift` Event（HarnessInternal，bounded payload：added/removed 名单
  上限 20）+ WARN 日志 + 指标 `buzhou.mcp.tools-drift`（server tag）；**基线推进为通知列表**
  （连续漂移各记各的）。空差量静默。

## 处置（M1 = 仅告警）

工具回调在会话装配期绑定，热替换需跨会话回调失效协议（大改，不做）——漂移告警后由运维
触发配置 refresh（`mcp.refresh` 既有差量重建）或重启会话吸收变更。文档与 runbook 明示。
