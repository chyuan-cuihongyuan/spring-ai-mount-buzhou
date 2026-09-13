# 800 — 事件丢弃按原因分类读面

> 来源：H 会话第 1 轮 = effort #800（[T1051](../../.wayfinder/tickets/T1051-event-drop-breakdown-shape.md) / [T1052](../../.wayfinder/tickets/T1052-event-drop-breakdown-verify.md) / impl 553）。借鉴：Sentry [discarded events](https://getsentry.github.io/sentry-relays/)——丢弃不只给总量，按 reason 分维可查询，"为什么丢"一读即知。

## Problem Statement

spec 13 §core-4 落地的 `EventBusStats.dropped` 只有**总量**：DROP_OLDEST 挤掉、BLOCK 超时、close 滞留、dispatcher 已关、中断五种丢弃原因仅存在于 WARN 日志文本（reason 参数），无结构化读面。运维视角无法回答"丢弃是容量不足（drop-oldest 多）还是停机排空（closed-undelivered 多）"——Sentry 的 discarded events 证明按原因分维是丢弃可观测的正确粒度。

## 目标

- `BufferedEventDispatcher.countDrop` 同步维护 reason→count 分类计数（`ConcurrentHashMap<String, LongAdder>`）；
- 新公共 record `EventDropBreakdown`（`core.session`，api 面）：不可变快照 `Map<String,Long> byReason()` + `forReason(String)` / `total()` 便捷访问；
- `AgentSession.eventDropBreakdown()` default 方法（默认 empty，与 `eventBusStats()` 同构）；`DefaultAgentSession` 接线 buffered 分发器；
- 守恒不变量：**breakdown 各原因计数之和 == `stats().dropped()`**（同一 countDrop 单点累计）；
- `EventBusStats` 原样不动（既有快照/兼容零变化）。

## 兼容性

纯增量 API：新公共类型 + 新 default 方法（默认 empty）。SYNC 模式与未升级实现零行为变化；既有 `EventBusStats` 语义不变。
