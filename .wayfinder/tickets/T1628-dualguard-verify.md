---
id: T1628
title: 双守卫组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1627
created: 2026-09-15
---

## Question

J 会话第 86 轮：双守卫组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（DualGuardReadoutTest）：混合调用后双读面各自守恒 + 互不串账 + reset 独立。定向 `mvn -pl buzhou-tools -am test -Dtest='DualGuardReadoutTest'` 绿。
