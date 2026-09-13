# 689 — webhook 死信环形上限

**What to build:** WebhookOutbox.MAX_DEAD_LETTERS=256 + evictOldestDeadIfFull（createdAt 升序丢最旧）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] MAX_DEAD_LETTERS 常量 + evictOldestDeadIfFull
- [x] DeadLetterCapTest（260→256 收敛/丢最旧/保留最新）
- [x] spec 937 + README 行（欠账累计 926–937）

## Done

验证：`mvn -pl buzhou-core test -Dtest=DeadLetterCapTest` 全绿。commit 见本轮 `feat(core)` 提交。
