# 453 — MCP 工具注解观测面与注解漂移

**What to build:** MCP server 自报的工具注解（readOnly/destructive/idempotent/openWorld hint + title）在注册表聚合可见（`registry.toolHints()` 快照零 RPC）；`tools/list_changed` 通知中同名工具注解翻转时发独立 `mcp.tool-hints-drift` 事件 + 指标 + WARN；名字差量口径零变化。

**Blocked by:** None — can start immediately.

**Status:** done

- [ ] `McpToolHints` record + `from(McpSchema.Tool)` null 安全映射
- [ ] `McpConnection.toolHints()` / `McpClientRegistry.toolHints()` default 空实现（伪连接零成本兼容）
- [ ] 工厂建连单次 `listTools` 同时缓存名字基线与 hints 基线
- [ ] 注册表 hints 差量：同名注解变化发 `mcp.tool-hints-drift`（payload 有界）+ `buzhou.mcp.tool-hints-drift` 指标；基线无 hints 跳过；空差量静默
- [ ] 聚合面只含 ACTIVE 条目
- [ ] 单测覆盖上述全部分支 + 既有 `McpToolsDriftTest` 零回归
- [ ] spec 600 + README 引用行

## Done

- [x] 全部验收标准满足；验证：`mvn -B -ntp -pl buzhou-mcp -am test` 绿（buzhou-mcp 41/41，含新增 McpToolHintsTest 6 用例；既有 McpToolsDriftTest 3/3 零回归；McpRealProtocolTest 2/2 真实协议过）。commit 见本轮 `feat(mcp)` 提交。
