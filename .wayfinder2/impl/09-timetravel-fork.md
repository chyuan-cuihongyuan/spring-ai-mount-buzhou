# 09 — core · time-travel fork（Completed-Turn 检查点）

**What to build:** 枚举历史 Completed-Turn 检查点并从任一检查点 fork 出新会话续跑——what-if 调试与评测的干净分叉点，原会话不受影响。

**Blocked by:** 07（事件溯源日志——检查点元数据与指纹设施）

**Status:** ready-for-agent

- [ ] `Session.listCompletedTurns()`：每检查点带历史指纹+元数据（时间、消息数、摘要引用）
- [ ] `forkFrom(turnId)`：复制截至该 Turn 的 history 到新 sessionId 下续跑新 Turn
- [ ] fork 后原会话读写不受影响（隔离断言）
- [ ] 与 fork 语义配套的事件可观测
- [ ] 端到端：fork→两会话分叉演进→各自历史一致
- [ ] spec（05 或恢复章节）同步

> spec 12 §core-6；[T34](../tickets/T34-core-interrupt-resume-timetravel.md)。源：langgraph 39,627★（get_state_history/update_state——Buzhou 以消息列表 state 免 channel 版本机制）。
