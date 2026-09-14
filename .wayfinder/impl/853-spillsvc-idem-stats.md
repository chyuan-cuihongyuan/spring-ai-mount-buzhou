# 853 — SpillService 幂等复用读面

**What to build:** SpillService 静态五计数（tryOffloadCalls/freshStores/idempotentReuses/degraded/belowThreshold）+ 嵌套 SpillServiceStats + stats()/resetForTest() + 五分支/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 五计数落点（入口/新落盘/幂等复用/降级/阈值内）
- [x] SpillServiceStats 嵌套 record + stats() + resetForTest()
- [x] SpillServiceStatsTest（五分支/守恒/reset 五测）
- [x] spec 1101 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='SpillServiceStatsTest'` 全绿 + 既有 SpillService 回归绿。commit 见本轮 `feat(spill)` 提交。
