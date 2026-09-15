# 873 — 压缩检查点操作读面

**What to build:** CompactionCheckpoints 静态两计数（saves/rollbacks）+ 嵌套 CheckpointStats + stats()/resetForTest() + 保存/回滚/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 两计数落点（save 入口/rollback 入口）
- [x] CheckpointStats 嵌套 record + stats() + resetForTest()
- [x] CheckpointStatsTest（保存/回滚/归零三测）
- [x] spec 1135 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='CheckpointStatsTest'` 全绿。commit 见本轮 `feat(memory)` 提交。
