# 01 — 背压地基 tracer bullet: 过载两档语义 + spawn 闸 + 容量异常 + e2e 骨架

**What to build:** 确立三维共用的过载两档策略词汇（`queue` 有界排队带超时，默认 / `fail-fast` 快速失败）与事件语义；新增容量拒绝异常类型（参照 `RuntimeDrainingException` 形态，message 带 sessionId + 当前活跃数 + 上限 + 已等待时长，异常规约）；spawn 闸最小闭环——`DefaultAgentRuntime` 复用既有 `liveSessions` 台账计数，超上限时按策略处置：queue 档虚拟线程 + 信号量有界等待空位（**禁止**轮询 sleep，会话 close 释放时通知），fail-fast 档直接拒绝；裁决点在租约获取**之前**（排队不持有租约，拿到空位后走既有 doSpawn 全流程）；`backpressure.spawn-queued`（当前活跃/上限）/ `backpressure.spawn-rejected`（原因：超时 / fail-fast）事件经既有事件通道发出（spawn 闸事件发生在会话建立前，参照 drain-started/finished 的 runtime 级直发先例）。core 侧新增 `buzhou.backpressure` 配置属性 record（boxed null=不限，对齐 `BuzhouShutdownProperties` 模板），本票接 `max-concurrent-sessions` / `spawn-queue-timeout` / `spawn-overload-policy`。同时确立**背压 e2e 测试骨架**（`CrashRecoveryEndToEndTest` 形态：`Buzhou.runtime(...)` + `ScriptedChatModel` + latch 阻塞工具 + 计数器），后续票据复用。

**Blocked by:** 无 — 可立即开始

**Status:** ready-for-agent

- [ ] e2e：上限=1 时第二 spawn 在 queue 档排队，第一会话 close 后放行成功；fail-fast 档直接抛容量异常
- [ ] 排队超时后抛容量异常，message 带 sessionId + 当前活跃数 + 上限 + 已等待时长
- [ ] 排队中的 spawn 不持有租约（排队期间同 sessionId 在另一 runtime 可正常 spawn）
- [ ] 不配置任何阈值时行为与现状完全一致（回归断言：null=不限）
- [ ] `backpressure.spawn-queued` / `backpressure.spawn-rejected` 事件按序出现在事件流，计数与上下文正确
- [ ] `buzhou.backpressure.max-concurrent-sessions` / `spawn-queue-timeout` / `spawn-overload-policy` yml 绑定生效
