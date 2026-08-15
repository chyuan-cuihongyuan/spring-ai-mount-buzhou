# Spec 25 — 熔断冷却自适应退避（韧性纵深）

> effort #6（T104 / impl-79）。延续 spec 15「熔断器」（T81）；借鉴 resilience4j
> wait-duration-in-half-open 的可配化思想 + TCP 指数退避语义，但保持手写零依赖。

## Problem Statement

熔断器冷却时长静态（`open-cooldown` 固定值）：上游持续故障场景下，每次冷却结束放行单探测、
探测立刻失败、再回 OPEN——「冷却→探测→立刻再跳」无效循环以固定周期反复锤故障 provider，
放大故障传导（生产事故常见形态：provider 挂 10 分钟，30s 冷却被打成 20 次探测冲击）。

## Solution

连续跳闸次数驱动的冷却指数退避：每次跳闸 `consecutiveTrips++`，生效冷却 =
`open-cooldown × min(2^(trips-1), backoff-cap)`；半开探测成功回 CLOSED 即 trips 复位。
首次跳闸冷却 = base（既有行为零变化）；provider 持续不恢复时探测节奏指数放缓，
恢复时一次探测成功即回到正常节奏。

## User Stories

1. As a SRE, I want provider 长故障时探测频率指数放缓, so that 熔断不把故障 provider 锤得更死。
2. As a SRE, I want provider 恢复后一次探测成功即复位退避, so that 恢复速度不受历史退避拖累。
3. As a 平台运维, I want 退避倍数有上限可配, so that 最长冷却窗口在掌控中（默认 8×open-cooldown）。
4. As a SRE, I want 跳闸事件携带连续次数与生效冷却时长, so that 告警面板能直接看出退避档位。
5. As a 应用开发者, I want 默认行为与旧版一致（首跳=base）, so that 升级零认知成本。

## Implementation Decisions

- **退避公式**：`multiplier = min(2^(trips-1), backoff-cap)`；`trips` 从 1 起（首跳 ×1）；
  cap 默认 8，可配 `buzhou.resilience.circuit.backoff-cap`（≥1 fail-fast）。
- **复位条件**：仅「HALF_OPEN 探测成功/IGNORED 回 CLOSED」复位 trips 与生效冷却；
  CLOSED 下正常新跳闸重新从 1 计（各次故障独立）。
- **生效冷却贯穿**：admit 的冷却判定/剩余时长、HALF_OPEN 探测占位期的拒绝 retryIn、
  探测超时逃生窗口（生效冷却 ×2）全部用生效值，不用 base。
- **可观测**：跳闸事件 payload 增 `consecutiveTrips`、`openDurationMs`（生效值）；
  指标 `buzhou.resilience.circuit-backoff-multiplier`（按 model 分桶 gauge，CLOSED 复位为 1）；
  ResilienceStats details 增 `circuitBackoff` 快照。
- **配置兼容**：`Circuit` record 增 `backoffCap` 组件 + 保留 6 参便捷构造（既有调用点零改动）。
- **不做二级自适应**：半开多探测/成功率阈值自适应不做（单 Agent 场景复杂度不抵收益，fog 记录）。
- **模块**：仅 `buzhou-resilience`（circuit + config 两类）。

## Testing Decisions

- 只测外部行为：状态迁移 + 拒绝异常携带的 retryIn 剩余时长（不窥视内部字段）。
- 用例矩阵：①连续探测失败冷却翻倍（100ms→200ms，事件 payload 断言 trips=2/openDurationMs=200）；
  ②探测成功复位（再跳闸冷却回 base）；③cap 封顶（cap=2 时第三次跳闸仍 ×2 不 ×4）；
  ④既有 12 用例全回归（首跳行为零变化）。
- 先例：`ModelCircuitBreakerTest`（sleep 过冷却驱动的半开用例风格）。

## Out of Scope

- 半开多探测、失败率驱动的动态阈值（fog：若多 agent 并发场景出现再评估）。
- 跨熔断器协调（主备各自退避，T82 降级链语义不变）。

## Further Notes

- 与 spec 15 的关系：本 spec 是其「OPEN 冷却」参数的动态化；跳闸判定口径（三态结果计数、
  IGNORED 语义）完全不变。
