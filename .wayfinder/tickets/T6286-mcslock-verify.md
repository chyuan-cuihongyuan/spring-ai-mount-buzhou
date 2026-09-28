---
id: T6286
title: T 会话 T43 MCS Lock 队列锁的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6285]
created: 2026-09-26
---

## Question

T43 合同怎么逐一验绿？（spec 6043 / effort #6043 / T43）

## Resolution

**验证通过**：McsLockTest 四测全绿——4×2000 互斥精确；解锁
后再锁；两线程交接时序；null fail-fast。
