# 814 — read_range 回读判定读面

**What to build:** ReadRangeTool 静态六计数（calls/reads/truncatedReads/parseRejects/skillRejects/failures）+ 嵌套 ReadRangeStats + stats()/resetForTest() + 回读/截断/拒绝/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 六计数落点（入口/完整/截断/catch/skill 两拒点合桶）
- [x] ReadRangeStats 嵌套 record + stats() + resetForTest()
- [x] ReadRangeStatsTest（回读/截断/坏 JSON/skill 拒/守恒/reset 六测）
- [x] spec 1062 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='ReadRangeStatsTest'` 全绿 + 既有 ReadRangeTool 回归绿。commit 见本轮 `feat(spill)` 提交。
