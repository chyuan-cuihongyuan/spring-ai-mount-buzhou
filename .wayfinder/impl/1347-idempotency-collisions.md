# impl 1347 — IdempotencyCollisions 幂等键冲突读面（R48 = effort #1747 / spec 1747 / T2695-T2696）

**What**：distinct 有界 FIFO+重放占比哨兵
**Why**：Stripe 幂等键遥测——键生成缺陷显形
**Verify**：IdempotencyCollisionsTest 3 断言 全绿。 **Status**：done（2026-09-15）
