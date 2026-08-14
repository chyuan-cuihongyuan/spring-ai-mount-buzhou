# 61 — MCP tools/list_changed 漂移检测（T86 决策落地）

**What to build:** McpConnectionFactory.connect(spec, listener) default 方法 + SpringAi 工厂挂 toolsChangeConsumer；McpConnection.listToolNames()；DefaultMcpClientRegistry 基线差量 + mcp.tools-drift 事件/日志/指标；McpObservability 增事件类型。

**Blocked by:** None.

**Status:** done

- [ ] McpConnectionFactory/McpConnection 接口扩展（default 方法，二进制兼容）
- [ ] SpringAiMcpConnectionFactory：toolsChangeConsumer 注册 + SpringAiMcpConnection.listToolNames()
- [ ] DefaultMcpClientRegistry：Entry 基线集 + handleToolsChanged 差量/告警/基线推进
- [ ] McpObservability：mcp.tools-drift 事件（bounded payload）+ 指标
- [ ] 测试：FakeMcp 工厂捕获 listener 注入变更（新增/移除/空差量/连续漂移）→ 事件与基线断言

## Done

验证：`./mvnw -pl buzhou-mcp clean test` 35/35 绿（新增 McpToolsDriftTest 3 用例：首漂移差量+同列表静默/连续漂移基线推进/条目下线迟到通知丢弃）。
落地：`McpConnectionFactory.connect(spec, listener)` default 方法（二进制兼容）+ `McpConnection.listToolNames()` default；SpringAi 工厂挂 SDK 2.0.0 `toolsChangeConsumer` + 连接实现 listToolNames（取不到基线=空集降级）；DefaultMcpClientRegistry Entry 基线集 + handleToolsChanged 差量（added/removed 有界 payload 20）+ WARN 日志 + `buzhou.mcp.tools-drift` 指标 + 基线推进；McpObservability 增 mcp.tools-drift EventType。M1 仅告警（热替换不做，文档明示处置=refresh/重启会话）。
