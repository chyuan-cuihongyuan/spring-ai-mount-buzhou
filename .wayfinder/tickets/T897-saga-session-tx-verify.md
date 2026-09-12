---
id: T897
title: per-session 事务域验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T896
created: 2026-09-12
---

## Question

事务域语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（SessionArchiverMutexTest 3/3 更新 + CompensatingBatchTest 7/7 + core 全模块零回归）：

- 跨会话双归档 maxInFlight=2（全局锁时代恒 1）——真并行；均完成归档键齐。
- 既有 saga 语义（补偿/失败上抛）零回归（无参重载直通）。
- 同会话互斥（622）不受影响。
