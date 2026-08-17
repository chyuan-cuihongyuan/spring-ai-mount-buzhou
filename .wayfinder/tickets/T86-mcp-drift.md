---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

MCP 工具集漂移检测怎么做？现状：refresh 仅由本地配置变更触发（replaceAll diff），server 端工具变更（协议 tools/list_changed 通知）完全不可见。决策点：list_changed 订阅的 SDK 能力核实（Spring AI MCP client 能力边界）、不可用时的轮询快照比对兜底（间隔/抖动/有界）、漂移事件与指标（mcp.drift detected/span）、处置策略（仅告警 vs 自动 refresh 重建 spec）、测试方案（进程内 MCP server 真协议，沿用 impl50 范式）。产出 spec 18 + impl 61。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **SDK 能力核实**：MCP Java SDK 2.0.0 `McpClient.SyncSpec.toolsChangeConsumer(Consumer<List<McpSchema.Tool>>)` 存在——协议级 `tools/list_changed` 订阅可用，**无需轮询兜底**（通知只在活连接上到达；轮询间隔兜底 M1 不做，诚实边界：连接断开期间的工具变更在下一次重连/refresh 重建时自然吸收）。
2. **事件驱动基线差量**：`McpConnectionFactory` 增 default `connect(spec, toolsChangedListener)`（默认忽略 listener，兼容既有实现/测试 fake）；`SpringAiMcpConnectionFactory` 覆写挂 `toolsChangeConsumer`。`McpConnection` 增 default `listToolNames()`（SDK 原始口径，建连基线）。
3. **差量与记账**：注册表 Entry 持 `toolNames 基线集`；通知到达→与基线差量（added/removed，M1 不比 description 变更——SDK 通知只给新列表，desc 差量噪音大）→非空差量发 `mcp.tools-drift` Event（HarnessInternal，bounded payload：added/removed 名单上限 20）+ WARN 日志 + 指标 `buzhou.mcp.tools-drift`（server tag）→**基线更新为通知列表**（连续漂移各记各的）。
4. **处置策略 M1 = 仅告警**：工具回调在会话装配期绑定，热替换需跨会话回调失效协议（大改）——auto-refresh 重建不做，文档明示「漂移告警后由运维触发配置 refresh 或重启会话」。
5. **测试**：FakeMcp 工厂捕获 listener、注入变更列表→断言事件/日志/基线推进/空差量静默；SDK 消费者注册属薄层不做真协议重测（impl50 真协议测试保持回归）。
