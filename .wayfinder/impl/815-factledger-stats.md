# 815 — 双时序事实台账操作读面

**What to build:** BiTemporalFactLedger 静态四计数（supersededWrites/historyLookups/validAtLookups/corruptRecordLoads）+ 嵌套 FactLedgerStats + stats()/resetForTest() + 写入/查询/损坏显形/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四计数落点（写入/历史读/时点读/load catch 处）
- [x] FactLedgerStats 嵌套 record + stats() + resetForTest()
- [x] FactLedgerStatsTest（写入/两查询/损坏显形/归零五测）
- [x] spec 1063 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='FactLedgerStatsTest'` 全绿 + 既有 BiTemporalFactLedger 回归绿。commit 见本轮 `feat(memory)` 提交。
