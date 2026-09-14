---
id: T2256
title: 计时聚合器双子实例清零面的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2255
created: 2026-09-15
---

## Question

M 会话第 3 轮：清零面如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-core test -Dtest=HookTimingAggregatorTest,ToolTimingAggregatorTest` 全绿——
① record 后 stats() 非空 → reset() → stats() 空映射（双子同款断言）；
② HookTimingAggregator 的 windowedMax() 同步清零；
③ reset() 幂等（连调两次无异常）；
④ Holder 既有 enable/reset（置 null）语义零变化（既有测试不动全绿）。
