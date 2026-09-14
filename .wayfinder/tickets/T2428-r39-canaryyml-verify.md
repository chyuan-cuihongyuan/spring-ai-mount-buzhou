---
id: T2428
title: R39 泄漏金丝雀 yml 装配的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2427
created: 2026-09-15
---

## Question

N 会话第 39 轮：如何验收？

## Resolution

LeakCanaryAssemblyTest 两断言：salt Builder 装配产物 hook 注册 + 种植/
跨会话检出（含 token 原样）/自回显不算泄漏；无 salt 构建零 SessionCanaryHook。
guard 370 用例零回归。
