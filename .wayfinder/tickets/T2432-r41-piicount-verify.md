---
id: T2432
title: R41 PII 豁免计数的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2431
created: 2026-09-15
---

## Question

N 会话第 41 轮：如何验收？

## Resolution

PiiExemptionTest 工具级豁免断言补 exemptionsApplied()==1（reset 归零后）；
guard 全量 370 用例零回归。
