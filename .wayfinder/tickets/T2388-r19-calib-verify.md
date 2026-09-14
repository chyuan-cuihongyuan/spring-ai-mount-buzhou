---
id: T2388
title: R19 校准审计接线的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2387
created: 2026-09-15
---

## Question

N 会话第 19 轮：如何验收？

## Resolution

CalibrationAuditHolderTest 两断言：成对入账语义（高估 20%+低估 20% → 均值误差 0、
偏高占比 0.5、actual=0 忽略）；install 替换与 null 重置。回归 TokenBudgetHook
端到端 5 用例 + 既有校准 5 用例零变化。
