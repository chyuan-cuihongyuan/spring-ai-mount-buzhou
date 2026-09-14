---
id: T2394
title: R22 会话检疫装配的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2393
created: 2026-09-15
---

## Question

N 会话第 22 轮：如何验收？

## Resolution

SessionQuarantineHookTest 两断言（MutableClock）：三连败后 beforeTurn Block +
snapshot remainingMillis 正 + 冷却 31s 后放行；健康会话 snapshot 零条目。
SessionQuarantineTest 6 用例零回归。
