---
id: T2186
title: 敏感性带计数与翻转分向的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2185
created: 2026-09-14
---

## Question

如何证明带计数、翻转分向与半开边界？

## Resolution

**用户常设授权 AFK（可推翻）**

`GateThresholdSensitivityTest` 五测全绿（`mvn -pl buzhou-core -am test`）：带内三分向精确（tighten 1/loosen 2）；**半开边界**（下界含端点、上界恰出——评审修正初版期望值错）；远分数稳健零带；δ 负值 fail-fast；空集 -1 哨兵。
