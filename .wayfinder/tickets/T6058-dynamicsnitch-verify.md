---
id: T6058
title: R 会话 R29 动态 snitch 的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6057]
created: 2026-09-23
---

## Question

R29 合同怎么逐一验绿？（spec 4028 / effort #4028 / R29）

## Resolution

**验证通过**：DynamicSnitchPenaltyTest 五测全绿——慢 50>10×1.5
罚（score 150）快免罚双证 + ranking 推尾；EWMA 单调收敛五步；
60 罚后连续 4ms×10 回落自动免罚；未见副本 NaN/不罚/不入
ranking；畸形五型 fail-fast。
