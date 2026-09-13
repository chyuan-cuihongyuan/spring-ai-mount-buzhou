# 702 — 断路器状态变迁事件流读数

> 来源：G 会话第 3 轮 = effort #702（15/25/57/620/638 熔断族读数深化）/ [T1004](../../.wayfinder/tickets/T1004-circuit-journal.md) / [T1005](../../.wayfinder/tickets/T1005-circuit-journal-verify.md) / impl 602。

## Problem

ModelCircuitBreaker 的状态变迁只以两个形态存在：当次调用会话的 `circuit.state-changed` 事件（无会话在飞时不可见）与聚合计数/gauge（只有当前值无历史）。排障问「过去一小时跳了几次闸、跳完多久恢复的、哪台模型反复跳」——只能翻散落在各会话事件流里的片段；巡检/复盘没有进程级变迁史。

## Solution

Resilience4j CircuitBreakerEvent 思想（≈50K star：事件流可订阅可聚合），本地落地为可查询 journal：

- `CircuitTransitionJournal`（resilience.circuit）：变迁环形留痕（容量 64，超出覆盖 `dropped` 计数），每条 `Transition(model, from, to, atEpochMs, consecutiveTrips, openDurationMs)`（后两项仅 to=OPEN 有值）。
- per-model 聚合：`tripsByModel`（→OPEN）、`recoveriesByModel`（→CLOSED）、`halfOpensByModel`（→HALF_OPEN）。
- `snapshot()` → 不可变 `Report(recent 最新在前, dropped, tripsByModel, recoveriesByModel, halfOpensByModel, capacity)`。
- 接线：内嵌 ModelCircuitBreaker（`transitionJournal()` getter——装配闭包既有捕获面可达），transition() 旁路 record，恒开零配置（有界内存，fsck health 恒 UP 同先例）。

## User Stories

1. 值班：告警响起来——读 journal snapshot 即见最近变迁序列（哪台模型、几次连跳、退避到几倍），不用拼各会话事件。
2. 复盘：tripsByModel 显示模型 X 一周反复跳——换供应商/调阈值的证据面。

## Implementation Decisions

- 恒开不做 yml 键（读数面非行为面；与 R1 audit 的区别：breaker 变迁频率低（跳闸是异常态），恒开成本可忽略；audit 的 admit 高频故只计数）。
- synchronized 写读（变迁低频，无争用热点）。
- 只记变迁不记拒绝（拒绝已有 `circuit-rejected` 计数+事件，镜像会造成双口径）。

## Testing Decisions

- 驱动真状态机：window=2/minCalls=2/threshold=50 双失败→OPEN；快进时钟→HALF_OPEN（admit 即探测）；探测成功×阈值→CLOSED——journal 三条变迁+聚合正确。
- 环形覆盖：容量小→dropped 计数+recent 保留最新。
- snapshot 防御拷贝；null model fail-fast。

## Out of Scope

- 事件外发订阅（push 语义——webhook/outbox 族已有通道，本面只管进程内查询）。
- core ToolCircuitBreaker 接线（工具级扩散留后续轮）。
- 变迁持久化（进程重启清零——连续史归日志面）。

## Further Notes

借鉴定源：resilience4j（≈50K）CircuitBreakerEvent；与 R1（OPA 决定留痕）同族不同面——audit 记「每次裁决」，journal 记「状态机跃迁」。
