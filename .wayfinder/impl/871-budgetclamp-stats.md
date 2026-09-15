# 871 — 预算钳位读面

**What to build:** DefaultBudgetCalculator 静态三计数（evaluations/negativeClamps/normalBudgets）+ 嵌套 BudgetClampStats + stats()/resetForTest() + 钳位/正常/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数落点（入口/钳位/正常）
- [x] BudgetClampStats 嵌套 record + stats() + resetForTest()
- [x] BudgetClampStatsTest（钳位/正常/守恒/归零四测）
- [x] spec 1133 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='BudgetClampStatsTest'` 全绿 + 既有预算回归绿。commit 见本轮 `feat(memory)` 提交。
