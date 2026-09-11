---
id: T922
title: 衰减过滤可观测的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

DecayingFactStore 过滤了多少事实（spec 626 装配后）不可读——「衰减在起作用」无编程信号。补吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 37 轮 = effort #600 / spec 636 / impl 489）：

1. `filteredCount()` getter（读时计数——与 fail-open 观测同模式）：非零持续增长 = 衰减确实在滤陈年低置信事实。
2. 不写回不加锁开销（AtomicLong increment 在过滤分支内）。
