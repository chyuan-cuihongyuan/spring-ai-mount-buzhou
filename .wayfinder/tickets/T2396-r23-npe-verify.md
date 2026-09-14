---
id: T2396
title: R23 对账 NPE 修复的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2395
created: 2026-09-15
---

## Question

N 会话第 23 轮：如何验收？

## Resolution

CounterAtomicitySpreadTest 恢复绿（M 会话记档的挂点）+ CalibrationAuditHolderTest
2 用例 + TokenBudgetHookEndToEndTest 5 用例零回归。
