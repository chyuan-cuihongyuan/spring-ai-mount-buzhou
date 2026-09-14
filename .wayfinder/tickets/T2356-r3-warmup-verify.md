---
id: T2356
title: R3 熔断启动宽限期的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2355
created: 2026-09-15
---

## Question

N 会话第 3 轮：如何验收？

## Resolution

`CircuitWarmupTest` 四断言（MutableClock）：宽限内灌满窗口不开闸且 suppressed=1、宽限后
下一次失败立即 OPEN；宽限内成功冲淡（4 失败+6 成功=0.4<0.5）宽限后不跳；默认关同序列
立即 OPEN（零变化）；负 warmup BuzhouConfigurationException fail-fast。
resilience 模块全量测试绿后单轮 commit。
