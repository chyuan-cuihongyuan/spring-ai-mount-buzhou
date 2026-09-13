# 783 — 会话级联清理聚合计数读面

**What to build:** SessionCleaner deleteCalls/cleanedTargets/failedTargets 三计数 + failuresByTarget 分桶 + 嵌套 CleanupStats + stats() + 直构测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数 + failuresByTarget 分桶
- [x] CleanupStats 嵌套 record + stats()
- [x] CleanupStatsTest（单会话/抛错贡献者/多会话累计/fresh 零值）
- [x] spec 1030 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core test -Dtest='CleanupStatsTest'` 全绿。commit 见本轮 `feat(core)` 提交。
