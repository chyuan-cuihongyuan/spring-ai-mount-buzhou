---
id: T2406
title: R28 PII 豁免双粒度的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2405
created: 2026-09-15
---

## Question

N 会话第 28 轮：如何验收？

## Resolution

PiiExemptionTest 三断言：工具级豁免（trusted_scraper）输出原样；类型级豁免
（type:CN_PHONE）EMAIL 脱敏 + 手机号原文保留；无豁免基线（EMAIL/CN_PHONE
全脱敏）。guard 全量 359 用例零回归。
