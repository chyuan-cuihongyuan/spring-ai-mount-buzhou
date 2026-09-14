# 1436 — 事件时序单调性审计

> 来源：L 会话第 37 轮 = effort #1436（票 T2173 / T2174 / impl 1089）。借鉴：事件溯源不变量（EventStoreDB：同流内时间戳单调是消费者正确性的前提）。

## Problem Statement

同会话事件的 occurredAt 应按存储序非递减——时钟回拨/并发写入乱序/重放注入会造成逆序：回放、审计、时序漂移计算等消费方的正确性会**静默劣化**（不算错、但结果错）。逆序无审计面。

## 目标

- `EventOrderAudit`（core/observability，纯函数静态面，private 构造）：
  - `analyze(List<EventRecord>)` → `record Report(totalEvents, inversions, maxInversionMillis, firstInversionIndex)`；
  - 逆序 = 相邻对 occurredAt 严格倒退（等时刻不算）；maxInversionMillis 倒退量；firstInversionIndex 定位（无逆序 -1）；
  - null 时戳跳过不崩（防御口径）；
  - 空输入零报告哨兵。
- 与 TurnSequenceAudit（cleanup 域 turn 序号缺号）辨义：那轴管轮号缺号，本轴管时间戳单调。
- 单会话口径（调用方按 sessionId 过滤后喂入）。

## 兼容性

纯函数零 IO；只读不裁决。

## Out of Scope

- 跨会话全局时序（多会话时钟域不可比——单会话口径显式）。
- 逆序修复/重排（读面不裁决）。
- span 层时序（span 有 startedAt 语义不同——另轴）。
