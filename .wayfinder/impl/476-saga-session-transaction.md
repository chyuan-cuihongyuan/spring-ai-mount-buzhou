# 476 — CompensatingBatch per-session 事务域

**What to build:** run(uow, sessionId, steps) 重载 + SessionArchiver 迁移（跨会话归档并行）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三参重载（补偿同域）+ archiver 迁移
- [x] 并行用例（maxInFlight=2）+ saga 零回归
- [x] spec 623 + README 行

## Done

验证：`mvn -pl buzhou-core test` 绿。commit 见本轮 `feat(core)` 提交。
