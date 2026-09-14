---
id: T2374
title: R12 熔断遥测接线的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2373
created: 2026-09-15
---

## Question

N 会话第 12 轮：如何验收？

## Resolution

CircuitTelemetryWiringTest 三断言（MutableClock）：三次 OPEN 达 crash-loop 闩锁
（minOpens=2 窗口语义钉死）；半开一败一成 ProbeStats 计数 + 恢复清闩；未注入
（withTelemetry 缺省）读数面 null 且行为零变化。resilience 380 用例零回归。
