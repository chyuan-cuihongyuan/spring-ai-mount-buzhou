---
id: T6113
title: S 会话 S7 Ticket Lock 票据锁的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

互斥等待怎么 FIFO 公平且排队深度可观测？（spec 5006 /
effort #5006 / S7）

## Resolution

**TicketLock（core/concurrent）**：Linux ticket spinlock——
lock() 原子取号自旋候号（先到先服务，饥饿结构性排除），
unlock(ticket) 校验放行（乱序 IAE），nowServing/queueLength
读数。
