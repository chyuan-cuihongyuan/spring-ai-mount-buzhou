# Wayfinder Map — Buzhou 时间旅行 fork（effort #311，C 会话第 12 轮）

> C 会话第 12 轮。fork（spec 20）只能从「最后消息」复制——回到过去某轮重走
> 分支（重试定位/探索对照）要手工裁剪历史。

## Destination

`AgentRuntime.forkFromTurn(source, appId, agentName, newSessionId, upToTurn)`：
复制 ≤ upToTurn 的历史前缀到新会话（LangGraph checkpointer time-travel）。
Summary 不复制（最新摘要可能覆盖 upToTurn 之后轮次——未来泄漏防护）；
State 不复制（预算重置同 fork）；fork 监听器/事件管线同 fork。

## Notes

- 号段：spec 311 / T613–T614 / impl-334。
- 借鉴：LangGraph checkpointer time-travel（从任意检查点重放）。

## Decisions so far

- upToTurn < 1 或前缀为空 → IllegalArgumentException（诚实拒绝）。
- 事件沿用 session.forked（payload 加 upToTurn）——监听器契约不变。

## Out of scope

- 摘要历史版重放（按轮对齐的摘要裁剪——归记忆族后续）；检查点显式化
  （轮号即检查点，无需额外存储）。

## Tickets

- [x] [T613 forkFromTurn 接口 + DefaultAgentRuntime 实现](../tickets/T613-time-travel.md)（impl-334）
- [x] [T614 E2E 回归（前缀复制/未来隔离/源不动/非法拒绝）](../tickets/T614-time-travel-close.md)（impl-334）
