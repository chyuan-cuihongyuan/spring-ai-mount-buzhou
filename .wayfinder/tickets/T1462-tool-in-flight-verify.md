---
id: T1462
title: 工具在飞并发水位读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1461
created: 2026-09-14
---

## Question

J 会话第 6 轮：在飞水位读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ToolInFlightTest，AssertJ 同仓风格）：enter 增 current/total；close 减 current；峰值只增不降（3 并发后 close 2 → current 1 peak 仍 3）；跨工具全局峰值；close 双调恰一次（AtomicBoolean）；reset 清零；快照 Map/List 不可变；隔离工具计数互不串。定向 `mvn -pl buzhou-core test -Dtest=ToolInFlightTest` 绿 + HookedToolCallback 触点回归（ToolDurationTimerTest）绿。
