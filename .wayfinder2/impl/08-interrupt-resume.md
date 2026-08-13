# 08 — core · HITL interrupt/resume 按 toolCallId 匹配

**What to build:** 人审挂起 Turn 后，resume 按 toolCallId 精确注入对应工具答复、多挂起可逐个 resume、绝不重放 Turn 前段——消除 LangGraph「resume 从头重执行」反模式。

**Blocked by:** 07（事件溯源日志——pendingToolCalls 持久化与同批已执行结果暂存落同一证据层）

**Status:** ready-for-agent

- [ ] Turn 挂起时记录 `pendingToolCalls[]`（toolCallId、args 指纹）并持久化
- [ ] `resume(toolCallId, payload)` 精确注入对应 ToolResponse；多个挂起可逐个 resume
- [ ] 绝不重放 Turn 前段：未落盘 Turn 丢弃重开、同批已执行工具结果按批记录暂存保留
- [ ] 与既有 HITL 门合流：挂起=门未放行、resume=授权 payload（语义一致）
- [ ] 端到端：双工具同批挂起→逐个 resume→Turn 完整完成且前段副作用零重放
- [ ] spec 05 / 07（Hook 护栏）同步

> spec 12 §core-6；[T34](../tickets/T34-core-interrupt-resume-timetravel.md)。源：langgraph 39,627★（interrupt/Command(resume)——按 toolCallId 匹配规避其 node 重执行与按序匹配缺陷）。
