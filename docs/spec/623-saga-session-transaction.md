# 623 — CompensatingBatch per-session 事务域

> 来源：F 会话第 24 轮 = effort #600（loop 23 勘察旁注的兑现）/ [T896](../../.wayfinder/tickets/T896-saga-session-tx-shape.md) / [T897](../../.wayfinder/tickets/T897-saga-session-tx-verify.md) / impl 476。

## 背景

CompensatingBatch.run 用 UnitOfWork 无参重载（全局 ReentrantLock）——全部 saga（含跨会话归档）全局串行；SPI 早有 per-session 重载未用。

## 目标

`run(uow, sessionId, steps)`：会话级事务域；SessionArchiver 迁移（跨会话归档并行）。

## 非目标

- 其他调用方不动（全局保守默认，各自按需迁移）。
- 不改 UnitOfWork SPI。

## 设计

inTransaction 辅助按 sessionId 分派；补偿步与正向步同域；null sessionId = 全局。

## 测试

跨会话双归档 maxInFlight=2（全局时代恒 1）；CompensatingBatchTest 零回归。

## 兼容性

无参重载直通（既有调用零变化）；SessionArchiver 行为面=吞吐提升。
