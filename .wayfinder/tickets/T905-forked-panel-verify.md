---
id: T905
title: 谱系面板验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T904
created: 2026-09-12
---

## Question

面板语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（SessionsEndpointForkedTest 2/2 + core 全模块零回归）：

- 真实 fork 的活跃分支计入 count=1、源会话不计（无谱系键）。
- 四参构造（无 state 面）available=false 诚实缺席。
