# 924 — 观测存储水位读面

> 来源：I 会话第 25 轮 = effort #924（[T1299](../../.wayfinder/tickets/T1299-obs-watermark-shape.md) / [T1300](../../.wayfinder/tickets/T1300-obs-watermark-verify.md) / impl 677）。借鉴：Redis `INFO memory`（used/peak vs maxmemory）——存量水位是容量治理的第一问。

## 背景

`InMemoryObservabilityStore` 有会话级采样逐出与丢弃计数（spec 13 有界纪律），但「当前存量 vs 上限」无读面——水位贴顶（逐出开始发生）不可见，只能在丢数据后从 dropped 计数反推。

## 目标

- `InMemoryObservabilityStore`（internal 包——非 API 面）新增 `watermark()`：
  - `record Watermark(int activeSessions, int maxSessions, long totalRecords, int maxRecordsPerSession, int sessionsEvicted)`；
  - activeSessions = 观测会话表大小；totalRecords = 各会话记录数合计；sessionsEvicted = 既有逐出累计直通；
  - synchronized 读一致性；
- 逐出/丢弃既有行为零变化（纯读面）。

## 兼容性

纯增量 internal 读面；零 API 面变化、零行为变化。
