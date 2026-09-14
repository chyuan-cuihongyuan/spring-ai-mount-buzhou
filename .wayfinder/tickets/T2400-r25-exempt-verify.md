---
id: T2400
title: R25 危险工具 HITL 豁免征询的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2399
created: 2026-09-15
---

## Question

N 会话第 25 轮：如何验收？

## Resolution

DangerousToolExemptionTest 四断言：有效豁免（+60s）beforeTool 放行；无豁免与
过期豁免（-1s）仍 Block；revoke 后恢复 Block；GuardModule.exemptions() 非空且
初始快照 active 空。guard 全量 343 用例零回归。
