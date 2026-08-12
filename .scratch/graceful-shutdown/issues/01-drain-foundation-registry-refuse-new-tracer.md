# 01 — drain 地基 tracer bullet: 会话台账 + 拒新 + 空载 drain e2e 骨架

**What to build:** runtime 追踪活跃会话（spawn 注册、close 注销，经既有 `onClose` 回调链挂上，不新增会话生命周期切面）；`AgentRuntime` 新增 `drain(timeout)` 编程式入口（additive default 方法）与拒新异常类型；drain 开始后 `spawn()` 抛携带 sessionId 与「实例正在 drain」上下文的拒新异常（不排队、不缓冲——拒绝即调用方的路由信号）；对无轮次在途的会话，drain 直接走正常 close（既有 `SessionResourceRegistry.closeAll()`：EXIT flush → 停心跳 → 关执行器 → 释租约）后返回；`drain-started`（活跃会话数）/ `drain-finished`（等完数/强杀数/总耗时）事件经既有事件通道发出；并发/重复触发 drain 幂等（状态机保证只生效一次，后续调用等待首次结果）。本票同时确立 **drain e2e 测试骨架**（`CrashRecoveryEndToEndTest` 形态：`Buzhou.runtime(...)` + `ScriptedChatModel` + latch 阻塞工具 + 计数器），后续票据复用。

**Blocked by:** 无 — 可立即开始

**Status:** ready-for-agent

- [ ] runtime 能回答「当前有哪些活跃会话」（spawn 注册 / close 注销，并发安全）
- [ ] drain 开始后 `spawn()` 抛拒新异常，异常 message 带 sessionId 与 drain 上下文（异常规约）
- [ ] 空载/仅空闲会话时 drain 返回后：会话已 close、租约已释放（同 sessionId 可立即再 spawn）
- [ ] `drain-started` / `drain-finished` 事件按序出现在事件流，计数正确
- [ ] 并发调用与重复调用 drain 只生效一次，后续调用得到同一结果（幂等）
- [ ] e2e 测试骨架落位（复用 CrashRecoveryEndToEndTest 装配形态，无 wall-clock sleep）
- [ ] `AgentRuntime` 接口扩展为 additive default 方法，既有实现与集成源码/二进制兼容
