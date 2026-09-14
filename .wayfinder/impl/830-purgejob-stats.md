# 830 — 归档清理任务读面

**What to build:** ArchivePurgeJob 静态三计数（purgeRounds/purgedTotal/skippedLocked）+ 嵌套 PurgeJobStats + stats()/resetForTest() + 清理/跳过/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数落点（入口/累计清理/锁跳过）
- [x] PurgeJobStats 嵌套 record + stats() + resetForTest()
- [x] PurgeJobStatsTest（清理/跳过/归零三测）
- [x] spec 1078 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='PurgeJobStatsTest'` 全绿 + 既有 ArchivePurgeJob 回归绿。commit 见本轮 `feat(core)` 提交。
