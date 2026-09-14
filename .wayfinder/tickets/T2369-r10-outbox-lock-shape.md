---
id: T2369
title: R10 WebhookOutbox 锁迁移的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2368
created: 2026-09-15
---

## Question

N 会话第 10 轮：outbox 的锁内 store IO 修复形状？

## Resolution

选 **ReentrantLock wrapper 迁移**（方法体抽 *Locked 零动——机械安全）。
outbox 是 dispatcher 虚拟线程的高频路径（默认 InMemory 无害、生产 JDBC/Redis 即
网络 IO in monitor），比 hook 家族优先。四方法共用一把锁（outbox 单写者语义）。
