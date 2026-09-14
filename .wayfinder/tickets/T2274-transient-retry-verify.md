---
id: T2274
title: 工具调用瞬断重试的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2273
created: 2026-09-15
---

## Question

M 会话第 13 轮：瞬断重试如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-core test -Dtest=ToolTransientRetryTest` 绿——
① 幂等工具 transient 异常（IOException 前两次、第三次成功）→ 重试后成功返回，退避计数正确；
② attempts 耗尽 → 原异常上抛（错误即反馈通道语义不变）；
③ 非幂等工具（无注解）transient 异常 → 零重试直接上抛（nonIdempotentSkipped 计数）；
④ 非瞬断异常（IllegalStateException）→ 零重试上抛；
⑤ Holder 未装配 → 装饰器不包（零开销透传）。
