# 05 — 会话级累计双窗口（跨崩溃持久化）

**What to build:** 会话生命周期的**累计**步数/工具调用数硬顶，持久化在 `SessionStateStore`，跨崩溃保留。会话级累计步数/工具调用数写 `SessionStateStore`（键 `runaway.session.steps`/`runaway.session.tool-calls`，参照 `recovery.autoresume.attempts` 读写先例）；`beforeModel`（步数）/`beforeTool`（工具调用）递增后校验会话级硬顶，超限返回 `Block(reason)`；`runaway.hard-stop` reason=session-steps/session-tool-calls。AUTO_RESUME 重驱动时计数**不重置**（避免崩溃-恢复循环重烧预算）；与 11 幂等去重协同（dedup 命中短路时不双重计数）。会话级窗口随会话删除而清除。

**Blocked by:** 01 — 步数变体复用其步数计数；工具调用变体受益于 03（`beforeTool` 计数基建）

**Status:** ready-for-agent

- [ ] e2e：预置 `sessionStateStore` 的 `runaway.session.steps` 接近上限后，下一轮触发 `runaway.hard-stop`（reason=session-steps）（照搬 `CrashRecoveryEndToEndTest.crashloopHardCapStopsRepeatedAutoResume` 形态）
- [ ] 跨崩溃保留：会话 close 后重 spawn（或 AUTO_RESUME）计数不重置
- [ ] 工具调用变体：`runaway.session.tool-calls` 同样生效（reason=session-tool-calls）
- [ ] dedup 协同：11 dedup 闸门命中短路的重复调用不被工具调用计数双重计算（核验 dedup 短路与 `beforeTool` 先后关系）
- [ ] 会话删除时计数随 store 生命周期清除
- [ ] 不配置时 null=不限（回归）
