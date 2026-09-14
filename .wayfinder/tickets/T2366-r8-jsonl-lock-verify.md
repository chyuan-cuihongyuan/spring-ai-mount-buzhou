---
id: T2366
title: R8 RollingJsonlWriter 锁迁移的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2365
created: 2026-09-15
---

## Question

N 会话第 8 轮：如何验收？

## Resolution

RollingJsonlWriterConcurrencyTest：8 虚拟线程 ×50 行并发追加——400 行计数守恒 +
行完整性（线程标记解析无撕裂）。回归 RollingJsonlWriterTest/GzipTest 全量零变化。
