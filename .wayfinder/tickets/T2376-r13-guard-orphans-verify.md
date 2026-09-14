---
id: T2376
title: R13 guard 孤类装配面的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2375
created: 2026-09-15
---

## Question

N 会话第 13 轮：如何验收？

## Resolution

GuardOrphanAssemblyTest 三断言：toolRoleGuard(permissions) 声明 → 注册
ToolRoleGuardHook；inputFloodGuard(config) 声明 → 注册 InputFloodGuardHook；
默认构建两 hook 均不注册（零行为）。guard 全量 334 用例零回归。
