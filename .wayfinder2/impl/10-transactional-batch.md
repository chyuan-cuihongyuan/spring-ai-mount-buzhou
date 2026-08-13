# 10 — core · 事务性并行批——批提交语义

**What to build:** 并行工具批「全部成功才整批入历史」；任一失败时失败者走错误回喂、成功者结果暂存批记录，回喂策略显式可配——诚实的状态层原子性，不谎称副作用回滚。

**Blocked by:** 07（事件溯源日志——成功者结果暂存落 ToolCallLog 批记录）

**Status:** ready-for-agent

- [ ] 并行批全部成功才把整批 ToolResponse 追加 history 并落 Completed-Turn
- [ ] 任一失败：失败者走 `ToolErrorFeedback`、成功者结果暂存批记录（ToolCallLog）
- [ ] 回喂策略显式可配（ALL=全部回喂 / FAILED_ONLY=仅失败回喂），默认值确定并有依据
- [ ] 命名与文档诚实：宣称「状态层原子」，明确副作用不回滚
- [ ] 端到端：3 工具并行 1 失败→两种策略各自的历史形状断言
- [ ] spec 05（并行工具）同步

> spec 12 §core-7；[T35](../tickets/T35-core-transactional-parallel-batch.md)。源：langgraph 39,627★ **修正版**语义（superstep=pending-writes 半事务、兄弟成功写保留）。
