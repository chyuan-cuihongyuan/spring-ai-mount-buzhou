---
id: T2696
title: 幂等键冲突读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2695
created: 2026-09-15
---

## Question

IdempotencyCollisions 怎么验证？（spec 1747 验收/裁决）

## Resolution

IdempotencyCollisionsTest：4 笔 2 重放 0.5/容量逐出/空哨兵。
