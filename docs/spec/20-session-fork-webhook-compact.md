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

- （待 T89 决议后补）

## 手动压缩 / 摘要导出（T90 / impl-65）

- （待 T90 决议后补）
