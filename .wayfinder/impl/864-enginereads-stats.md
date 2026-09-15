# 864 — RangeReadEngine 引擎读面

**What to build:** RangeReadEngine 静态四计数（engineCalls/windowReads/jsonReads/byteReads）+ 嵌套 EngineReadStats + stats()/resetForTest() + 三模式/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四计数落点（read switch 三模式分支 + 入口）
- [x] EngineReadStats 嵌套 record + stats() + resetForTest()
- [x] EngineReadStatsTest（三模式/守恒/reset 四测）
- [x] spec 1113 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='EngineReadStatsTest'` 全绿。commit 见本轮 `feat(spill)` 提交。
