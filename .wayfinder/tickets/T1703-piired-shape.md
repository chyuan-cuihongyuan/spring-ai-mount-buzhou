---
id: T1703
title: PII 出站脱敏读面（PiiEventRedStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1697
created: 2026-09-15
---

## Question

J 会话第 122 轮：pii 出站面读面的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：PiiEventRedactor（事件出站脱敏装饰器）零计数——事件处理量/脱敏改写/无命中透传/fail-open 透传四路径分布不可见（fail-open 高发=脱敏面失效信号）。纯测试轮。

形状裁决：`PiiEventRedactor` 内静态 `AtomicLong` 四计数——eventsProcessed（onEvent 入口）/ redacted（有命中改写下发）/ cleanPassthrough（无命中原样）/ failOpen（脱敏异常透传）；嵌套 `record PiiEventRedStats` + `stats()` + `resetForTest()`。守恒 `eventsProcessed = 三结局桶之和`。静态面理由同族先例；onEvent/redactPayload 返回语义逐位不变。

Out of scope：按 type 分桶（PiiHitStats 已覆盖）；嵌套递归深度（留档即边界）。
