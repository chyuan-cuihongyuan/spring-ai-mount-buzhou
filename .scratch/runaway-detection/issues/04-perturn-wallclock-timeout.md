# 04 — 单轮 wall-clock 超时（步边界）

**What to build:** 单轮 wall-clock 硬顶，在步边界生效。`beforeTurn` 记录轮次起始 `Instant`；`beforeModel` 检查 `Duration.between(start, now) > per-turn.wall-clock` → 返回 `Block(reason)`；`runaway.hard-stop` reason=wall-clock。**诚实边界**写进机制文档与 spec：轮次时长上界 = `wall-clock + 单步时长`（一次模型调用延迟 + 一次工具超时），**非中途精确打断**；中途 watchdog-cancel（独立线程到点调 `session.cancel()`）为潜在增强，不在本票。wall-clock 是**轮次级**，与 10 韧性单步 `ResilienceProperties.deadline`（单次模型调用级）正交共存。

**Blocked by:** 01 — 复用其 Hook 骨架（`beforeTurn`/`beforeModel`）与 e2e 装配

**Status:** ready-for-agent

- [ ] e2e：`per-turn.wall-clock=短值` + `BlockingTool`（或 `BlockingChatModel`）时触发 `runaway.hard-stop`（reason=wall-clock）（参照 `ResilienceEndToEndTest.deadlineTimeoutFiresAndCancelsInFlightCall` 形态，**不用** wall-clock sleep）
- [ ] 终止原因回复含 wall-clock 维度 + 上限 + 实际耗时
- [ ] 诚实边界（上界 = deadline + 单步时长，非中途打断）写入 spec 与机制文档
- [ ] wall-clock 与 10 韧性单步 `deadline` 正交共存（不相互误清零 in-flight 注册）
- [ ] 不配置时 null=不限（回归）
