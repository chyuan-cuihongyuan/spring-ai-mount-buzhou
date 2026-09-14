# 1100 — 事件去重聚合读面

**What to build:** EventDeduplicator 增量（passed/deduped 双计数+DeduplicationStats+resetStatsForTest 只清计数）+ 三测。

**Blocked by:** None.

**Status:** done

- [x] EventDeduplicator 埋点（onEvent 双分支计数）
- [x] EventDeduplicationStatsTest 三测
- [x] spec 1448 + README 行（既有类增量——快照面不变）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='EventDeduplicationStatsTest'` 3/3 绿。
