---
id: T869
title: 项级超时验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T868
created: 2026-09-12
---

## Question

超时语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（EvalItemTimeoutTest 3/3）：

- 挂死项（latch 永不开）+ 100ms 预算：该条 error（detail 带项超时+预算）、健康项照跑 pass、run 完成计 1 error 1 pass。
- 并行路径（parallelism=2 双挂死）：整跑预算内完成、双 error。
- 校验：零/负预算拒绝、null 关闭合法。
