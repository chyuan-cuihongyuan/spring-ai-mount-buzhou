# 1211 — PII 出站脱敏读面

> 来源：J 会话第 122 轮 = effort #1222（编号修正：本 spec 对应轮 122；[T1703](../../.wayfinder/tickets/T1703-piired-shape.md) / [T1704](../../.wayfinder/tickets/T1704-piired-verify.md) / impl 876）。借鉴：出站网关脱敏覆盖率（fail-open 频次是可用性与安全权衡的实时信号）。pii 域出站面（PiiHitStats 命中面之外的路径分布）。

## Problem Statement

`PiiEventRedactor.onEvent`（事件出站脱敏装饰器）四路径——脱敏改写、无命中透传、脱敏异常 fail-open 透传、事件处理——零计数：**脱敏面健康与 fail-open 频次不可见**。

## 目标

- `PiiEventRedactor` 增量（guard/pii，静态面）：四 `AtomicLong`。
  - `eventsProcessed`：onEvent 入口；`redacted`（有命中改写下发）/ `cleanPassthrough`（无命中原样）/ `failOpen`（脱敏异常透传）三结局桶。
- 嵌套 `record PiiEventRedStats(...)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**eventsProcessed = redacted + cleanPassthrough + failOpen**。

## 兼容性

纯增量读面：onEvent 转发语义、fail-open 原文下发逐位不变；静态面理由同 R46–R121 先例；无新配置项。

## Out of Scope

- 按 PiiType 分桶（PiiHitStats 已覆盖）。
- 嵌套递归深度（留档即边界）。
