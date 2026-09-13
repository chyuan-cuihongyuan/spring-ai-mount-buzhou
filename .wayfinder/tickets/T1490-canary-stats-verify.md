---
id: T1490
title: 金丝雀生命周期计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1489
created: 2026-09-14
---

## Question

J 会话第 20 轮：金丝雀生命周期计数如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（CanaryGuardStatsTest，复用 InjectionDefenseUnitTest 的 MutableModelCallContext shim 与载荷构造）：播撒幂等（两次 beforeModel planted=1）；泄漏拦截 leaked=1；变体自硬化 variantBlocked=1；无辜输出三计数不动。定向 `mvn -pl buzhou-guard test -Dtest='CanaryGuardStatsTest,InjectionDefenseUnitTest'` 绿。
