# 05 — core · 显式取消 CancelMode 三档 + token 贯穿

**What to build:** 取消运行中的请求时可选「立即 / 等当前工具批 / 等当前 Turn」三档语义；取消贯穿嵌套工具链；部分更新不泄漏进历史。

**Blocked by:** None — can start immediately.

**Status:** done（2026-08-14：CancelMode 三档 + AgentSession.cancel(mode) + manager requestCancel/pendingCancel + advisor 取消护栏（IMMEDIATE/AFTER_CURRENT_TOOLS 截断、AFTER_CURRENT_TURN 放行）+ CancellationToken 随 ToolContext 贯穿；CancelModeEndToEndTest 3 例含令牌翻转断言；spec 05 新节）

- [ ] `enum CancelMode { IMMEDIATE, AFTER_CURRENT_TOOLS, AFTER_CURRENT_TURN }`（AutoGen 两档 + 中间档）
- [ ] IMMEDIATE=虚拟线程 interrupt、丢弃在飞工具结果；AFTER_CURRENT_TOOLS=等 StructuredTaskScope join 后停；AFTER_CURRENT_TURN=完整落 Completed-Turn（部分输出保留语义）
- [ ] 取消 token 贯穿 TurnLoop 与工具执行器；`InterruptibleTool` 可选接口让长任务主动检查
- [ ] 取消事件可观测（含档位与部分保留说明）
- [ ] 端到端：三档各自的部分结果保留/丢弃语义断言
- [ ] spec 05（并行工具）同步

> spec 12 §core-3；[T31](../tickets/T31-core-cancel-mode.md)。源：autogen 60,404★（CancellationToken+partial 丢弃+ExternalTermination）+ openai-agents-python 28,616★。
