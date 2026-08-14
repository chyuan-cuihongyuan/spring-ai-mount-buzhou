# 50 — mcp 真实协议测试与运维面

**What to build:** MCP 连接工厂获得真实协议层集成测试（SDK 进进程 server：握手/listTools/调用/断连/STDIO）；ToolSetSpecStore bean 自动接线 DB 清单源；连接状态健康+指标+日志；close 有总预算；外部 MCP 危险工具按客户端模式自动登记进 guard HITL。

**Blocked by:** 44-resilience-module-port

**Status:** ready-for-agent

- [ ] SpringAiMcpConnectionFactory 集成测试：StreamableHttp（SDK InMemory transport）+ STDIO 进程（覆盖握手/listTools/toolcall/断连）
- [ ] AutoConfiguration 接线 ObjectProvider<ToolSetSpecStore>
- [ ] 配置 fail-fast + properties 化 + JSR-303 + 元数据
- [ ] 健康（ACTIVE/DRAINING/CLOSED/最近失败）+ 指标（mcp.connections.* 并入 core 家族）+ 日志基线
- [ ] close() 总预算可配（默认 35s）
- [ ] dangerousToolNamePatterns + 注册表 dangerousToolNames() + examples 装配侧接 guard HITL e2e
