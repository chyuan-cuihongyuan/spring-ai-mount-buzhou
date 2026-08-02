# 01 — 工程骨架与会话脊柱

**What to build:** 16 模块 Maven 骨架（父 POM/BOM/core/starter 占位）+ GitHub Actions CI 就位；`AgentRuntime.spawn(appId, agentName)` 与 `Buzhou.enhance(ChatClient.Builder)` 双层入口可用，AgentSession 支持 chat/stream 多轮对话（此票不带任何 Harness 机制，纯直通 Spring AI）；消息经内存版五 SPI 全保真落库、可读回，sessionId 直接作为 conversationId。

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] `mvn verify` 全绿，16 模块结构成型，CI 跑通
- [ ] demo：spawn 一个 Agent 进行 3 轮对话，历史从内存 MessageStore 完整读回（含 tool_calls 结构）
- [ ] 会话资源注册表就位：close() 触发成套清理钩子（本票先注册租约/执行器位）
- [ ] 包结构 api/internal 边界与 buzhou-* 命名符合 09 spec
