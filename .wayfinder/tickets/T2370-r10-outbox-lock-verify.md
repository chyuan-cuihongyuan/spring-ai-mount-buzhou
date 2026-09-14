---
id: T2370
title: R10 WebhookOutbox 锁迁移的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2369
created: 2026-09-15
---

## Question

N 会话第 10 轮：如何验收？

## Resolution

webhook 包全量 105 用例零回归（互斥语义由既有行为测试覆盖：容量/退避/孤儿审计/
死信迁移）。锁迁移是语义保持变换，无需新增并发用例。
