# 693 — 工具调用结局分布读面

**What to build:** ToolCallOutcomeStats（四桶纯函数统计 + total 守恒）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ToolCallOutcomeStats.stats + OutcomeStats record
- [x] OutcomeStatsTest（四桶精确/守恒/空日志/null fail-fast）
- [x] spec 944 + README 行（欠账累计 926–944）

## Done

验证：`mvn -pl buzhou-core test -Dtest=OutcomeStatsTest` 全绿。commit 见本轮 `feat(core)` 提交。
