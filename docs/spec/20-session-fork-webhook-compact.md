# Spec 20 — 会话 fork / 事件外发 / 手动压缩（mechanisms）

> effort #5（T88–T90 / impl-63~65）。借鉴 LangGraph time-travel fork、OpenHands event stream、
> GitHub webhook（HMAC+重试+幂等）、Claude Code /compact。

## 会话 fork（T88 / impl-63）

- **`AgentRuntime.fork(sourceSessionId, appId, agentName, newSessionId)`**（default UOE，
  DefaultAgentRuntime 实现）：从源会话**最后消息**完整复制历史到新会话（指定 messageId 截断
  M1 不做——截断点选择需另设计）。
- **复制语义**：Message 全量复制（BuzhouMessage 不可变共享引用）；Summary 只复制**最新一版**
  （保工作记忆连续性，分支后各自演化）；**SessionState 不复制**（runaway/budget/quota 是会话
  生命周期预算——fork = 重试/探索 = 预算重置）。
- **新会话语义**：走完整 spawn 管线（容量闸/租约/装配 customizer 全生效），复制在 spawn 成功后
  执行；源会话不动、两分支独立演化。
- **evidence/spill**：天然共享只读（按路径引用）；源会话删除级联删 spill 导致分支引用失效为
  已知边界（文档明示）。
- 源无消息历史 → IllegalArgumentException；停机期拒绝（SHUTDOWN_INTERRUPTED）。
- 事件 `session.forked`（sourceSessionId）；指标 `buzhou.session.forks`。

## 事件外发 webhook（T89 / impl-64）

- **`core/webhook/WebhookEventForwarder`**（`SessionEventListener`）：配置 `buzhou.webhook.url` 才装配
  （默认关）。core 增**全局监听挂点**：`DefaultAgentRuntime.addGlobalEventListener`（新会话自动挂 +
  已活跃补挂）；core auto-config 收集 `SessionEventListener` bean 注入。
- **投递语义**：at-least-once（幂等键 `eventId`=UUID 每请求 `X-Buzhou-Event-Id`；消费方按需去重，
  exactly-once 不承诺）；单虚拟线程分发器 + 有界队列（默认 256）**满则丢弃 + 计数**（不阻塞主链）；
  优雅关闭限时排空。
- **签名**：`X-Buzhou-Signature: hex(HMAC-SHA256(secret, body))`（配置 secret 才带）。
- **重试**：IOException/5xx 退避 1s×2^n（默认 3 次）；4xx 不重试；全败丢弃 + 计数。
- **HTTP**：JDK HttpClient（零新依赖），POST JSON `{eventId, sessionId, type, payload, occurredAt}`，
  timeout 默认 5s。指标 `buzhou.webhook.delivered / dropped / failures`。

## 手动压缩 / 摘要导出（T90 / impl-65）

- **宿主侧 `memory` 模块 `ManualCompactor`**（`compact(sessionId)` / `exportSummary(sessionId)` /
  `exportSummaryMarkdown(sessionId)`）：压缩机制归 memory（core 不持 memory 组件）；模型侧
  CompactNowTool 重构为**委托同一条管线**（行为零变化）。
- **幂等**：alreadyCovered / summarizedMessageIds 跳过已折入；无待折返回 skipped（CompactResult
  携带统计：折入数/代际/估算 token）。
- **并发边界**：不做锁——SummaryStore 版本化追加 + 幂等集与在途轮消息追加天然并发安全；
  建议轮间隙调用（轮中调用安全但摘要可能少折最后一轮）。
- **导出**：类型化 `Optional<NineSectionSummary>` 或渲染 Markdown；无摘要 = empty。
- 装配：`MemoryModule.manualCompactor()`（配置摘要模型才可用）+ auto-config bean。
