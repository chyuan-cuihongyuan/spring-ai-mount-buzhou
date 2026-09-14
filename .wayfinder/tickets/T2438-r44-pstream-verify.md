---
id: T2438
title: R44 流式 PII 豁免的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2437
created: 2026-09-15
---

## Question

N 会话第 44 轮：如何验收？

## Resolution

PiiStreamExemptionTest 两断言（chunk 化喂入）：豁免 CN_PHONE 后 EMAIL 跨
chunk 照脱（[PII:EMAIL]）+ 手机号原文保留；无豁免双脱（EMAIL+CN_PHONE 占位）。
guard 372 用例零回归。
