---
id: T2365
title: R8 RollingJsonlWriter 锁迁移的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2364
created: 2026-09-15
---

## Question

N 会话第 8 轮：锁内磁盘 IO 修复选哪种形状——ReentrantLock 迁移还是专用写线程？

## Resolution

选 **ReentrantLock 迁移**。专用写线程+有界队列是吞吐改造（引入背压/丢弃语义，
超出 pinning 修复范围）；锁迁移互斥语义零变、测试零改动、先例明确
（HarnessToolCallingManager 已注迁移说明）。吞吐瓶颈出现时再独立立项写线程化。
