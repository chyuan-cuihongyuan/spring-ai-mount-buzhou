---
id: T2390
title: R20 spill 写速率限速的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2389
created: 2026-09-15
---

## Question

N 会话第 20 轮：如何验收？

## Resolution

SpillWriteRateLimiterTest 五断言（时间断言用宽松界防 CI 抖动）：burst 吸收
立即可写 + 超速节流发生；超时放行 degraded 计数；关闭态 <10ms 零开销；
store 集成节流不破写入；配置校验。spill 全量 180 用例零回归。
