# 792 — todo 动作分布读面

**What to build:** TodoTool 白名单五桶动作分布计数 + 嵌套 TodoActionStats + actionStats() + 分桶测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 五桶计数（list/upsert/remove/clear/other）
- [x] TodoActionStats 嵌套 record + actionStats()
- [x] TodoActionStatsTest（分桶/未知归 other/累计/不可变/fresh 零值）
- [x] spec 1040 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-tools test -Dtest='TodoActionStatsTest,TodoToolTest'` 全绿。commit 见本轮 `feat(tools)` 提交。
