# 1400 — 跨会话轮次并发水位观察者 + guard-block 轮观察者收口修复

> 来源：L 会话第 1 轮 = effort #1400（[T2101](../../.wayfinder/tickets/T2101-turn-concurrency-shape.md) / [T2102](../../.wayfinder/tickets/T2102-turn-concurrency-verify.md) / impl 1053）。借鉴：HikariCP 池读面（active/idle 连接数 + 历史峰值水位——「此刻池里有多少连接、峰值到过多少」是池治理的第一读数）。

## Problem Statement

会话内轮次单飞（spec 40 §B：CAS 0→1），真实并发维度在**跨会话**——但应用级「此刻有多少轮在途、峰值到过多少、累计开/成/败多少」全无读面：`inFlightTurns()` 是 per-session 点读数（runtime 排空裁决用），无跨会话聚合、无峰值、无生命周期总量。

**同轮实证缺陷**：`DefaultAgentSession` 两处 beforeTurn-Block 路径（非流式 `doChatTurn` / 流式 guard 拒绝）已派发 `onTurnStart` 却**永不派发终结回调**——观察者视角该轮永无终态，TURN span 泄漏（impl-30 全线防 span 泄漏的契约缺口）；`TurnConcurrencyTracker` 守恒式 `started = okFinished + failed + active` 亦无法闭合。

## 目标

- `TurnConcurrencyTracker implements SessionObserver`（core/session，实例面）：同实例注册到全部会话即跨会话聚合。
  - 三总量：`started` / `okFinished`（onTurnEnd）/ `failed`（onTurnError）；
  - `active`（AtomicInteger 在途）+ `peakActive`（accumulateAndGet 单调峰值水位）；
  - 守恒式 `started = okFinished + failed + active`（终结回调重复派发防御：active 不为负）；
  - `stats()` 只读快照 + `resetForTest()`（装配级单例归零口，BuzhouMetricsHolder 先例）。
- **缺陷修复**（同轮）：非流式 guard-block 补 `onTurnEnd(turnSeq, block.reason())` + 计时（outcome=ok——block 亦产出最终回复）；流式 guard-block 补 `onTurnError(turnSeq, ISE(reason))`（与订阅者所见 error 终结对称）。护栏拒绝返回语义逐位不变。

## 兼容性

Tracker 纯 opt-in（不注册零开销）；`Snapshot` 为嵌套 record 不进 API 快照面变化（外层类入面随轮再生）。缺陷修复只增派观察者回调——`SessionObserver` 全 default 方法，既有实现无感知；护栏拒绝的返回值/异常路径逐位不变。计时补录（outcome=ok）使 block 轮进入既有 `buzhou.turn.duration` 计时族（先前缺漏的补全）。

## Out of Scope

- per-session 峰值（跨会话聚合已覆盖治理诉求；per-session 活跃已有 `inFlightTurns()`）。
- 模型调用级并发（模型调用随轮内工具循环多次发生，语义归 TurnTimingHook/HookTimingAggregator 族）。
- guard-block 轮计时单列 outcome=blocked 档（有界枚举扩展留后续按需）。
