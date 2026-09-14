---
id: T2426
title: R38 慢调用 yml 装配的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2425
created: 2026-09-15
---

## Question

N 会话第 38 轮：如何验收？

## Resolution

CircuitSlowCallAssemblyTest 三断言：12 参构造组归一（500ms/0.8）+ 6 参
兼容缺省（null + 0.5）；负 duration 与 1.5 rate 各自 fail-fast（消息含键名）；
withSlowCallPolicy(100ms, 0.8) 下 5 次慢成功 → OPEN。
resilience 396 用例零回归。
