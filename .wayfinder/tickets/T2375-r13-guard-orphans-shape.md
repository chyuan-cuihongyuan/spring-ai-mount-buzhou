---
id: T2375
title: R13 guard 孤类装配面的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2374
created: 2026-09-15
---

## Question

N 会话第 13 轮：guard 两孤类修复——编程面还是连 yml 面？

## Resolution

选 **编程面先行**。Builder 声明即注册是最小可用装配路径（宿主能挂上=机制生效）；
yml 绑定面（permissions map 结构解析）是锦上添花后续轮。ToolRoleGuardHook 是安全
边界机制（fail-closed），优先级高于读数类孤类。
