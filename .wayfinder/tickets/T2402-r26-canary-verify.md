---
id: T2402
title: R26 泄漏金丝雀接线的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2401
created: 2026-09-15
---

## Question

N 会话第 26 轮：如何验收？

## Resolution

SessionCanaryHookTest 两断言：两会话种植后 B 输出含 A 令牌 → 一条泄漏事件 +
detectedCount=1 + A 自回显零事件；同会话重复种植 size=1（确定性）。
SessionCanaryRegistryTest 4 用例 + guard 全量零回归。
