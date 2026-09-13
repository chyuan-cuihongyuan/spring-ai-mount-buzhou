# 681 — 评估 run 状态分布查询读面

**What to build:** EvalQueryService.runsWithPruned（pruned 项筛选 + PrunedRunSummary 投影降序）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] runsWithPruned + PrunedRunSummary
- [x] PrunedQueryTest（筛选/计数/降序/空 store/既有零回归）
- [x] spec 928 + README 行（欠账累计 906–928 二十三行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=PrunedQueryTest` 全绿。commit 见本轮 `feat(core)` 提交。
