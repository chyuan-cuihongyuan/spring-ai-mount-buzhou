---
id: T6182
title: S 会话 S41 Blocked Sliding Counter 分块滑窗计数器的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6181]
created: 2026-09-25
---

## Question

S41 合同怎么逐一验绿？（spec 5040 / effort #5040 / S41）

## Resolution

**验证通过**：BlockedSlidingCounterTest 四测全绿——小窗钉住
（位 11 [2,3] 含真值 3）；500 位扫描真值逐位落界且界宽 ≤10；
零流恒零；参数 fail-fast。
