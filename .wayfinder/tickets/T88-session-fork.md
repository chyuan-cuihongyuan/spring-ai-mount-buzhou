---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

会话 fork/branch 怎么做？借鉴：LangGraph time-travel fork（checkpoint 历史取任意点分支重放）。现状：快照存储已在（snapshots 表、RunStateSnapshot），resume 有，fork 无。决策点：fork 源（从最后消息 vs 指定 messageId 截断）、store 层复制语义（Message/Summary/SessionState 三 store 的 copy-to-new-session）、evidence-id/spill 产物归属（共享只读）、新 sessionId 语义与事件、与 memory 压缩历史的关系。产出 spec 20 + impl 63。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **API**：`AgentRuntime.fork(sourceSessionId, appId, agentName, newSessionId)`（default UOE；DefaultAgentRuntime 实现）——M1 从**最后消息**完整复制（指定 messageId 截断不做：BuzhouMessage 无稳定 messageId 排序语义，截断点选择需另设计；fog 记录）。
2. **复制语义**：Message 全量复制（BuzhouMessage 不可变，共享引用 append）；Summary 只复制**最新一版**（保工作记忆连续性，不带版本历史包袱——分支后各自演化）；SessionState **不复制**（runaway/budget/quota 是会话生命周期预算，fork = 重试/探索 = 预算重置，正是想要的语义）。
3. **新会话语义**：走完整 spawn 管线（容量闸/租约/装配/自定义 customizer 全生效）——fork 不是旁路；复制在 spawn 成功后执行。源会话不动（可继续用，两分支独立演化）。
4. **evidence/spill 归属**：天然共享只读（spill 文件按路径引用，evidence-id 指针跨会话可读）——不复制、不搬家，文档明示「源会话删除会级联删 spill，分支引用失效为已知边界」。
5. **源校验**：源会话无消息历史 → IllegalArgumentException（fork 空会话无意义）；停机期 fork 拒绝（同 spawn SHUTDOWN_INTERRUPTED）。
6. **事件**：新会话发 `session.forked`（sourceSessionId payload）；指标 `buzhou.session.forks`。
