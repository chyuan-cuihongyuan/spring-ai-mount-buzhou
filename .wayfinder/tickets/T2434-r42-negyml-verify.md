---
id: T2434
title: R42 负缓存 yml 装配的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2433
created: 2026-09-15
---

## Question

N 会话第 42 轮：如何验收？

## Resolution

NegativeCacheYmlAssemblyTest 三断言（ApplicationContextRunner）：
enabled=true → bean 存在且上下文关闭后 enabled=false（钩子停用）；ttl=2m
经 Holder 读数生效；缺省 doesNotHaveBean + enabled=false。
