# 827 — 会话归档操作读面

**What to build:** SessionArchiver 静态四计数（archiveCalls/archived/emptySkipped/pdbRejected）+ 嵌套 ArchiveStats + stats()/resetForTest() + 三结局/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四计数落点（入口/成功/空会话/pdb 拒）
- [x] ArchiveStats 嵌套 record + stats() + resetForTest()
- [x] ArchiveStatsTest（成功/空会话/归零等测）
- [x] spec 1075 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='ArchiveStatsTest'` 全绿 + 既有 SessionArchiver 回归绿。commit 见本轮 `feat(core)` 提交。
