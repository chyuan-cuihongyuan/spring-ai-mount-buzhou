---
id: T2304
title: 批级回喂预算的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2303
created: 2026-09-15
---

## Question

M 会话第 29 轮：批预算如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：BatchResponseBudgetTest 三用例（manager 级直测）+ HarnessToolCallingManagerTest 12 用例零回归——① 预算 60 批内大 100+小 20：大者截断带标记、小者完整；② 未设预算 10K 结果完整透传；③ 总量未超限零截断。
