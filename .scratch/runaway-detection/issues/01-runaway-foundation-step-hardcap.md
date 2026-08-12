# 01 — 失控检测地基 tracer：配置 + Hook 骨架 + 轮次级步数硬顶 + 硬顶携带部分结果 + e2e 骨架

**What to build:** 确立失控检测的共用词汇与 e2e 测试骨架，并跑通第一个完整闭环——**轮次级步数硬顶**。新增 `buzhou.runaway` 配置属性 record（`enabled` + `per-turn.max-steps`，boxed 类型 null=不限，对齐 `BuzhouBackpressureProperties` 模板）；新增 `RunawayHook` 骨架（`beforeTurn` 重置轮次计数、`beforeModel` 每次模型调用递增步数并在 `nextCall` 之前校验步硬顶，超限返回 `Block(reason)`，reason 含维度/上限/当前值/部分结果摘要）；`runaway.hard-stop` 事件（reason=steps）经既有 `emitEvent` 通道（参照 `backpressure.*` 命名先例）；硬顶 Block 经 `HookAdvisor` 既有路径成为本轮最终回复；硬顶后本轮已完成工具调用结果随 unit-of-work 落库（**携带部分结果**）。确立后续票复用的 e2e 形态：`Buzhou.runtime(model, stores, config)` + core test-jar `ScriptedChatModel` + `session.addEventListener(events::add)` + `messageStore.load(sid)`。本票即「让后续变更变容易」的地基——后续票复用此处的 properties 形态、Hook 骨架与 e2e 装配。

**Blocked by:** 无 — 可立即开始

**Status:** ready-for-agent

- [ ] e2e：`per-turn.max-steps=3` 且 ScriptedChatModel 预排 ≥3 个 tool-call message 时，第 4 次模型调用未发起（`model.seenPrompts.size()==3`）
- [ ] `runaway.hard-stop` 事件出现，payload reason=steps、limit/value 正确
- [ ] 硬顶后最终回复含终止原因文本（可解释终止，非黑屏）
- [ ] 硬顶后 `stores.messageStore().load(sid)` 含本轮已完成的部分工具结果（携带部分结果，照搬 `GracefulShutdownEndToEndTest.drainForceKillsAndFlushesExitTierBufferedWrites` 断言形态）
- [ ] 硬顶后会话不废：同一 session 再次 `chat` 仍正常响应（与 drain/租约单活跃语义正交）
- [ ] 不配置任何阈值（`max-steps=null`）时行为与现状完全一致（回归断言：零计数开销不改变推理循环）
- [ ] `buzhou.runaway.enabled` / `per-turn.max-steps` yml 绑定生效；`enabled=false` 时机制完全旁路（等价现状）
