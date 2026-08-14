# 63 — 会话 fork（T88 决策落地）

**What to build:** AgentRuntime.fork default + DefaultAgentRuntime 实现（Message 全量 + Summary 最新一版复制；State 不复制）+ session.forked 事件 + 指标。

**Blocked by:** None.

**Status:** done

- [ ] AgentRuntime.fork default（UOE）
- [ ] DefaultAgentRuntime.fork：源校验（无历史抛 IAE）→ spawn 完整管线 → 复制消息/摘要 → session.forked 事件 + buzhou.session.forks 指标
- [ ] 测试：fork 后新会话带完整历史继续对话（prompt 含源会话上下文）；分支独立演化（互不影响）；State 不复制（预算重置）；空源抛 IAE；事件断言

## Done

验证：`./mvnw -pl buzhou-core clean test` 261/261 绿（新增 SessionForkEndToEndTest 4 用例：历史复制续聊/分支独立演化/State 不复制预算重置/空源拒绝+事件）。
落地：AgentRuntime.fork default（UOE）+ DefaultAgentRuntime 实现（源校验 IAE → 完整 spawn 管线 → Message 全量 + Summary 最新一版复制，State 不复制）+ DefaultAgentSession.dispatchEventInternal 包级入口 + session.forked 事件 + buzhou.session.forks 指标。spill/evidence 共享只读（源删除级联为已知边界，spec 20 明示）。
