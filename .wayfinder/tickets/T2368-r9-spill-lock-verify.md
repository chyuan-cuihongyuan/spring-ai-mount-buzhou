---
id: T2368
title: R9 DiskSpillStore 锁迁移的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2367
created: 2026-09-15
---

## Question

N 会话第 9 轮：如何验收？

## Resolution

DiskSpillStoreConcurrencyTest 两断言：同 uri 6 虚拟线程并发 store 恰 1 成功
5 拒绝；异 uri 8 并发全成功 + entryCount 守恒。spill 模块 168 用例零回归。
