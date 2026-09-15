# 1113 — RangeReadEngine 引擎读面

> 来源：J 会话第 116 轮 = effort #1116（[T1691](../../.wayfinder/tickets/T1691-enginereads-shape.md) / [T1692](../../.wayfinder/tickets/T1692-enginereads-verify.md) / impl 864）。借鉴：存储引擎操作分布（引擎原语层与工具端到端层分离对账）。spill 引擎层首轴。

## Problem Statement

`RangeReadEngine.read`（切片引擎静态原语：bytes/window/json 三模式，read_range 工具与 spill 回读共用）零计数——**引擎级三模式占比**无口径（R62 工具层是端到端，含 toolName/会话语义；引擎层是多调用方共享原语——不同轴）。

## 目标

- `RangeReadEngine` 增量（spill，静态面）：四 `AtomicLong`。
  - `engineCalls`：read 入口；`byteReads`（BYTES）/ `jsonReads`（JSON）/ `pageReads`（PAGE）。
- 嵌套 `record EngineReadStats(long engineCalls, long byteReads, long jsonReads, long pageReads)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**engineCalls = byteReads + jsonReads + pageReads**。

## 兼容性

纯增量读面：read 返回语义、窗口/JSON 求值逐位不变；静态面理由同 R46–R115 先例；无新配置项。

## Out of Scope

- 切片长度分布（展示面）。
- jsonPath 求值失败细分（失败已透传文本）。
