# impl 1494 — IdempotencyKeyGuard 幂等键判定面（R94 = effort #1893 / spec 1893 / T2987-T2988）

**What**：`IdempotencyKeyGuard`（core/transaction 静态纯函数 +
Decision 枚举）——decide（FIRST 首见/REPLAY 同键同参/CONFLICT 同
键异参）+ isExpired（TTL 恰到期失效）；负 TTL/负时点 fail-fast。

**Why**：Stripe idempotency-key 三态——只判键存在会漏「复用旧键改
参数」的副作用错配；CONFLICT 即 idempotency_error 语义。与 501
IdempotencyAdvisor（存储重放面）互补：那是管线这是决策语义。

**Verify**：`IdempotencyKeyGuardTest` 4 用例全绿（三态/缺指纹复用
CONFLICT/TTL 边界两例/畸形两型 fail-fast）。

**Status**：done（2026-09-23）
