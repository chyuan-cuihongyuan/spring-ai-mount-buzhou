# 21 — guard · FIDES 最小 taint 信息流控制（标 + 写门校验）

**What to build:** 不可信数据带 taint 标签、未经审批不得流入写侧工具调用：读侧打标、LLM 响应保守 join 传播、写门校验（失败走既有 HITL/物理阻断）——读写非对称的形式化终点（MVP）。

**Blocked by:** 12（memory 防投毒——TaintLabel 与 provenance 同源）

**Status:** done（2026-08-14：TaintTrackingHook（读侧打标 UNTRUSTED、保守单调、持久化跨续接、显式消毒）+ TaintWriteGateHook（order 250 写门：tainted 上下文写侧调用拦截转 HITL、审批复用既有授权台账=FIDES approver 等价）+ GuardModule.taintTracking() 开关；TaintWriteGateEndToEndTest 2 例（AgentDojo 式拦截/approve 放行/trusted 零扰动）；guard 37/37 绿；spec 07 新节 + 纵深序更新）

- [ ] `TaintLabel`（枚举起步 TRUSTED/UNTRUSTED + 来源）读侧 hook 给工具/RAG 输出 Attachment 打标（与 memory provenance 同源）
- [ ] LLM 响应保守取输入标签 join 传播进会话状态
- [ ] 写门（物理阻断 + HITL 工具调用前）校验「上下文标签 ⊔ 实参标签」，失败走既有 session-state 授权 + TTL（= FIDES approver 等价物）
- [ ] AgentDojo 式端到端：untrusted 上下文中诱导的写侧调用被写门拦截/转 HITL；trusted 正常流不受扰
- [ ] taint 拦截事件可观测；二期（变量隐藏/隔离 LLM）留 fog 不做
- [ ] spec 07（Hook 护栏）同步

> spec 12 §guard-21；[T49](../tickets/T49-guard-fides-minimal-taint.md)。源：MSRC FIDES 论文注记（AgentDojo 注入归零、效用损失 4.5–16.2%）。
