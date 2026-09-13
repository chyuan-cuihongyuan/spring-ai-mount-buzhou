---
id: T1488
title: taint 信息流控制生命周期计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1487
created: 2026-09-14
---

## Question

J 会话第 19 轮：taint 生命周期计数如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（TaintLifecycleStatsTest，InMemorySessionStateStore + DefaultToolCallContext 直构骨架）：打标首计 firstMarks=1、重复打标只增 marksApplied；写门四桶——非写侧工具不计、可信放行计 allowedTrusted、tainted 未批准计 blocked（Block 文案含「信息流控制」）、人工批准后计 allowedApproved 且放行；守恒 checked == trusted + approved + blocked。定向 `mvn -pl buzhou-guard test -Dtest='TaintLifecycleStatsTest,TaintWriteGateEndToEndTest'` 绿。
