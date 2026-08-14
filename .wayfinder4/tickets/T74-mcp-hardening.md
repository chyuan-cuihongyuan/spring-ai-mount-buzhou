---
Type: grilling
Status: closed
blocked-by: T69
---

## Question

buzhou-mcp 的生产级收口范围：真实协议层集成测试（SDK 内嵌 server 验证 SpringAiMcpConnectionFactory 握手/listTools/STDIO 进程）、AutoConfiguration 接线 ObjectProvider<ToolSetSpecStore>（DB 源可用）、配置校验+元数据、连接状态健康/指标（ACTIVE/DRAINING/inFlight/失败计数）、日志基线、shutdown 时长上限接入停机预算、MCP 工具危险名单登记衔接 guard、工具快照重发现是否本轮做。

## Resolution

进本轮（采纳 T69 §3 + 本地勘察）：
1. **真实协议层集成测试**：用 MCP Java SDK 内嵌 StreamableHttp server（SDK 自带 InMemory transport 亦可接受）做握手/listTools/工具调用/断连的进程内集成测试，覆盖 SpringAiMcpConnectionFactory（HTTP 形态）；STDIO 形态用 SDK 的 stdio server 进程拉起验证。不引 Testcontainers（无官方镜像且重）。
2. AutoConfiguration 接线 `ObjectProvider<ToolSetSpecStore>`——DB 清单源在装配路径可用。
3. 配置校验+元数据：fromYml/parseSpec 的裸解析补 fail-fast（transport 枚举、endpoint 非空、timeout 正数）；ResilienceProperties 化 + JSR-303 + additional-metadata。
4. 健康/指标：BuzhouHealthIndicator（ACTIVE/DRAINING/CLOSED 连接数、最近建连失败）+ MeterBinder 预注册（mcp.connections.active 等族）。
5. 日志基线：refresh 差量 INFO、建连失败 WARN、forceClose WARN、shutdown 排空 INFO。
6. shutdown 时长：close() 总预算上限可配（默认 35s≈grace+1s，超出放弃等待仅强杀），接入停机预算注记。
7. **MCP 危险工具登记**：`dangerousToolNamePatterns`（客户端侧配置，默认 delete/drop/write/update/send/remove/exec 类动词模式），经注册表聚合暴露 `dangerousToolNames()`，starter/examples 装配侧与 tools 模块的 enabledDangerousToolNames() 同挂点接 guard HITL。
8. 工具快照重发现：本轮不做（注记开放问题，已有 spec 注记）。RefCountingToolCallback 拒绝文案改英文并对齐 ToolErrorFeedback 语义。（可推翻）
