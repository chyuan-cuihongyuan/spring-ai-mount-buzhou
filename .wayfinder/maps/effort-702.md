# effort #702 — 断路器状态变迁事件流读数

- 会话：G 会话 700 系第 3 轮 ｜ spec [702](../../../docs/spec/702-circuit-transition-journal.md) ｜ 票 [T1004](../tickets/T1004-circuit-journal.md)/[T1005](../tickets/T1005-circuit-journal-verify.md) ｜ impl602
- 借鉴：Resilience4j（≈50K star）CircuitBreakerEvent/EventConsumer——状态变迁事件流可订阅可查询

## 勘察（排重）

- ModelCircuitBreaker 变迁仅走当次调用会话事件通道（EVENT_STATE_CHANGED）+ 计数/ gauge——**进程级「最近跳了谁/恢复了几次」无查询面**（无会话在飞时跳闸史不可见）。
- 504 snapshot / 638 timeWindow 读面是状态快照，非变迁历史。
- core ToolCircuitBreaker 是工具级另一类（跨模块扩散留后续轮）。
- grep TransitionJournal：零命中。

## 决定

`CircuitTransitionJournal`（resilience.circuit，内嵌 breaker 恒开——纯读数有界内存）：变迁环形 64 条 `Transition(model, from, to, atEpochMs, consecutiveTrips, openDurationMs)`+per-model trips/recoveries/halfOpens 计数+dropped 覆盖计数+snapshot() 不可变 Report；`ModelCircuitBreaker.transitionJournal()` getter 暴露（customizer 闭包既有捕获面可达）。

## 测试

直接驱动 breaker 触发 OPEN→HALF_OPEN→CLOSED 全链/journal 记录与聚合/环形覆盖/不可变。

## 诚实边界

会话事件通道语义不变（本面是旁路镜像）；只记状态变迁不记拒绝（拒绝已有计数+事件）；core 工具级断路器不接线。
