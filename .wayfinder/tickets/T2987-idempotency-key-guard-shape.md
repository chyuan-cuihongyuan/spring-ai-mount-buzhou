---
id: T2987
title: 幂等键判定面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

幂等键的三态判定与 TTL 失效怎么语义化？（spec 1893 / effort #1893 / R94）

## Resolution`

**Stripe idempotency-key 三态纯决策 `IdempotencyKeyGuard`
（core/transaction）**：decide（存档缺席 FIRST/指纹相等 REPLAY/
不等 CONFLICT——同键异参拒绝）+ isExpired（now≥created+ttl 失效，
边界恰到期即失效）。null 存档=首见口径；ttl/时点非负 fail-fast。
与 501 Advisor 存储重放面互补不撞。
